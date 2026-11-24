package com.magnetixdian.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.application.pago.ComprobantePagoDto;
import com.magnetixdian.application.pago.ServicioComprobante;
import com.magnetixdian.application.pago.ServicioEntitlement;
import com.magnetixdian.application.pago.ServicioWebhookBilling;
import com.magnetixdian.application.pago.WebhookResultDto;
import com.magnetixdian.domain.pago.PagoAprobadoEvent;
import com.magnetixdian.domain.pago.ValidadorFirmaWebhook;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.pago.BillingCheckoutJpa;
import com.magnetixdian.infrastructure.pago.BillingCheckoutRepository;
import com.magnetixdian.infrastructure.pago.BillingWebhookEventJpa;
import com.magnetixdian.infrastructure.pago.BillingWebhookEventRepository;
import com.magnetixdian.infrastructure.pago.LectorEventoPagoWebhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookBillingTest {

    private static final String PAYLOAD = """
            {"eventId":"evt-1","referencia":"WOMPI-REF","estado":"APPROVED","monto":99000,"moneda":"COP"}
            """;

    private BillingCheckoutRepository checkoutRepo;
    private BillingWebhookEventRepository eventoRepo;
    private ServicioComprobante comprobantes;
    private ApplicationEventPublisher publicador;
    private ServicioWebhookBilling servicio;

    @BeforeEach
    void setUp() {
        checkoutRepo = mock(BillingCheckoutRepository.class);
        eventoRepo = mock(BillingWebhookEventRepository.class);
        EmpresaRepository empresaRepo = mock(EmpresaRepository.class);
        ServicioEntitlement entregas = mock(ServicioEntitlement.class);
        comprobantes = mock(ServicioComprobante.class);
        publicador = mock(ApplicationEventPublisher.class);

        BillingCheckoutJpa checkout = new BillingCheckoutJpa();
        checkout.setReferencia("WOMPI-REF");
        checkout.setEmpresaId(1L);
        checkout.setPlanCode("PRO");
        checkout.setProveedor("WOMPI");
        checkout.setMonto(new BigDecimal("99000"));
        checkout.setMoneda("COP");
        when(checkoutRepo.findByReferencia("WOMPI-REF")).thenReturn(Optional.of(checkout));

        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(1L);
        when(empresa.getNit()).thenReturn("900123456");
        when(empresaRepo.findById(1L)).thenReturn(Optional.of(empresa));

        when(comprobantes.generar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ComprobantePagoDto("CP-000001", LocalDate.now(), "900123456", "PRO",
                        new BigDecimal("99000"), new BigDecimal("15798"), "PAGADO", "/pdf"));

        servicio = new ServicioWebhookBilling(new ValidadorFirmaWebhook("", "", false, "test"),
                new LectorEventoPagoWebhook(new ObjectMapper()), checkoutRepo, eventoRepo, empresaRepo,
                entregas, comprobantes, publicador);
    }

    @Test
    void webhookAprobadoGeneraComprobanteYEvento() {
        when(eventoRepo.findByProveedorAndEventId("WOMPI", "evt-1")).thenReturn(Optional.empty());

        WebhookResultDto resultado = servicio.procesar("WOMPI", PAYLOAD, Map.of());

        assertEquals("APROBADO", resultado.estado());
        assertEquals("CP-000001", resultado.comprobanteNumero());
        assertFalse(resultado.duplicado());
        verify(comprobantes).generar(any(), any(), any(), any(), any(), any());
        verify(publicador).publishEvent(any(PagoAprobadoEvent.class));
        verify(eventoRepo).save(any(BillingWebhookEventJpa.class));
    }

    @Test
    void webhookDuplicadoNoVuelveAGenerarComprobante() {
        WebhookResultDto aprobado = servicio.procesar("WOMPI", PAYLOAD, Map.of());
        BillingWebhookEventJpa registrado = new BillingWebhookEventJpa();
        registrado.setProveedor("WOMPI");
        registrado.setEventId("evt-1");
        registrado.setReferencia("WOMPI-REF");
        registrado.setResultado("APROBADO");
        registrado.setComprobanteNumero(aprobado.comprobanteNumero());
        when(eventoRepo.findByProveedorAndEventId("WOMPI", "evt-1")).thenReturn(Optional.of(registrado));

        WebhookResultDto repetido = servicio.procesar("WOMPI", PAYLOAD, Map.of());

        assertTrue(repetido.duplicado());
        assertEquals("CP-000001", repetido.comprobanteNumero());
        verify(comprobantes, times(1)).generar(any(), any(), any(), any(), any(), any());
        verify(publicador, times(1)).publishEvent(any(PagoAprobadoEvent.class));
    }

    @Test
    void montoInferiorAlEsperadoSeRechaza() {
        String payload = PAYLOAD.replace("99000", "1000");
        when(eventoRepo.findByProveedorAndEventId("WOMPI", "evt-1")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> servicio.procesar("WOMPI", payload, Map.of()));
        verify(comprobantes, never()).generar(any(), any(), any(), any(), any(), any());
    }
}
