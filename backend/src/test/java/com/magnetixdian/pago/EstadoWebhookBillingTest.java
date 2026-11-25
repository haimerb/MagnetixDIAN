package com.magnetixdian.pago;

import com.magnetixdian.application.pago.ComprobantePagoDto;
import com.magnetixdian.application.pago.EstadoWebhookPagoDto;
import com.magnetixdian.application.pago.ServicioComprobante;
import com.magnetixdian.application.pago.ServicioEstadoWebhookBilling;
import com.magnetixdian.infrastructure.pago.BillingCheckoutJpa;
import com.magnetixdian.infrastructure.pago.BillingCheckoutRepository;
import com.magnetixdian.infrastructure.pago.BillingWebhookEventJpa;
import com.magnetixdian.infrastructure.pago.BillingWebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EstadoWebhookBillingTest {

    private BillingCheckoutRepository checkoutRepo;
    private BillingWebhookEventRepository eventoRepo;
    private ServicioComprobante comprobantes;
    private ServicioEstadoWebhookBilling servicio;

    @BeforeEach
    void setUp() {
        checkoutRepo = mock(BillingCheckoutRepository.class);
        eventoRepo = mock(BillingWebhookEventRepository.class);
        comprobantes = mock(ServicioComprobante.class);
        servicio = new ServicioEstadoWebhookBilling(checkoutRepo, eventoRepo, comprobantes);

        BillingCheckoutJpa checkout = new BillingCheckoutJpa();
        checkout.setReferencia("WOMPI-REF");
        checkout.setProveedor("WOMPI");
        checkout.setEstado("APROBADO");
        when(checkoutRepo.findByReferencia("WOMPI-REF")).thenReturn(Optional.of(checkout));
    }

    @Test
    void estadoSinWebhookDevuelvePendienteSinFirma() {
        when(eventoRepo.findByReferenciaOrderByCreatedAtDesc("WOMPI-REF")).thenReturn(List.of());

        EstadoWebhookPagoDto estado = servicio.estado("WOMPI-REF");

        assertEquals("APROBADO", estado.estado());
        assertFalse(estado.firmado());
        assertEquals(null, estado.eventId());
        assertTrue(estado.mensaje().contains("todavía no fue recibido"));
    }

    @Test
    void estadoConWebhookMuestraFirmaEventoYComprobante() {
        BillingWebhookEventJpa evento = new BillingWebhookEventJpa();
        evento.setEventId("evt-1");
        evento.setReferencia("WOMPI-REF");
        evento.setResultado("APROBADO");
        evento.setFirmado(true);
        evento.setMensaje("Pago aprobado.");
        when(eventoRepo.findByReferenciaOrderByCreatedAtDesc("WOMPI-REF")).thenReturn(List.of(evento));
        when(comprobantes.porReferencia("WOMPI-REF")).thenReturn(Optional.of(new ComprobantePagoDto(
                "CP-000001", LocalDate.now(), "900123456", "PRO", new BigDecimal("99000"),
                new BigDecimal("15798"), "PAGADO", "/pdf")));

        EstadoWebhookPagoDto estado = servicio.estado("WOMPI-REF");

        assertEquals("WOMPI", estado.proveedor());
        assertEquals("APROBADO", estado.estado());
        assertTrue(estado.firmado());
        assertEquals("evt-1", estado.eventId());
        assertEquals("CP-000001", estado.comprobanteNumero());
        assertEquals("Pago aprobado.", estado.mensaje());
    }

    @Test
    void referenciaDesconocidaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> servicio.estado("NO-EXISTE"));
    }
}
