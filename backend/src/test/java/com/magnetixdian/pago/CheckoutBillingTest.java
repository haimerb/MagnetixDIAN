package com.magnetixdian.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.application.PasarelasPago;
import com.magnetixdian.application.pago.CheckoutRequestDto;
import com.magnetixdian.application.pago.CheckoutResponseDto;
import com.magnetixdian.application.pago.ServicioCheckoutBilling;
import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.MetodoPago;
import com.magnetixdian.domain.pago.PasarelaPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.PlanJpa;
import com.magnetixdian.infrastructure.persistence.PlanRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.infrastructure.pago.BillingCheckoutJpa;
import com.magnetixdian.infrastructure.pago.BillingCheckoutRepository;
import com.magnetixdian.infrastructure.pago.PasarelaWompi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutBillingTest {

    private BillingCheckoutRepository checkoutRepo;
    private EmpresaRepository empresaRepo;
    private UsuarioRepository usuarioRepo;
    private PlanRepository planRepo;
    private PasarelasPago pasarelas;
    private ServicioCheckoutBilling servicio;

    @BeforeEach
    void setUp() {
        checkoutRepo = mock(BillingCheckoutRepository.class);
        empresaRepo = mock(EmpresaRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        planRepo = mock(PlanRepository.class);
        when(checkoutRepo.save(any(BillingCheckoutJpa.class))).thenAnswer(i -> i.getArgument(0));

        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(1L);
        when(empresaRepo.findById(1L)).thenReturn(Optional.of(empresa));

        PlanJpa plan = mock(PlanJpa.class);
        when(plan.getCodigo()).thenReturn("PRO");
        when(plan.getNombre()).thenReturn("Profesional");
        when(plan.getPrecioMensual()).thenReturn(new BigDecimal("99000"));
        when(plan.getMoneda()).thenReturn("COP");
        when(planRepo.findByCodigoIgnoreCase("PRO")).thenReturn(Optional.of(plan));

        pasarelas = new PasarelasPago(List.of(pasarelaWompi("pub-test", "integrity-test")));
        servicio = new ServicioCheckoutBilling(checkoutRepo, empresaRepo, usuarioRepo, planRepo,
                pasarelas, "http://frontend.test");
    }

    @Test
    void checkoutCreaReferenciaYQuedaPendienteConPasarelaReal() {
        CheckoutResponseDto respuesta = servicio.iniciar(new CheckoutRequestDto("PRO", 1L,
                "admin@magnetix.test", "WOMPI"));

        assertNotNull(respuesta.referencia());
        assertTrue(respuesta.referencia().startsWith("WOMPI-"));
        assertNotNull(respuesta.checkoutUrl());
        assertEquals("PENDIENTE", respuesta.estado());
        assertEquals(new BigDecimal("99000"), respuesta.monto());
        assertEquals("COP", respuesta.moneda());
    }

    @Test
    void checkoutRechazaPasarelaSimulada() {
        CheckoutRequestDto solicitud = new CheckoutRequestDto("PRO", 1L, "admin@magnetix.test", "SIMULADO");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.iniciar(solicitud));

        assertTrue(error.getMessage().contains("SIMULADO"));
    }

    @Test
    void checkoutSinCredencialesDePasarelaDevuelveErrorExplicito() {
        ServicioCheckoutBilling sinCredenciales = new ServicioCheckoutBilling(checkoutRepo, empresaRepo,
                usuarioRepo, planRepo, new PasarelasPago(List.of(pasarelaWompi("", ""))), "http://frontend.test");
        CheckoutRequestDto solicitud = new CheckoutRequestDto("PRO", 1L, "admin@magnetix.test", "WOMPI");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> sinCredenciales.iniciar(solicitud));

        assertTrue(error.getMessage().contains("no está configurada"));
        assertTrue(pasarelas.obtener("WOMPI").configurada());
    }

    @Test
    void checkoutQrPropagaDatosDelPagadorYRespuesta() {
        PasarelaPago pasarela = mock(PasarelaPago.class);
        when(pasarela.proveedor()).thenReturn("WOMPI");
        when(pasarela.configurada()).thenReturn(true);
        when(pasarela.crearCheckout(any())).thenReturn(CheckoutPago.qr("qr-1", "BASE64", "tx-1"));
        ServicioCheckoutBilling servicioQr = new ServicioCheckoutBilling(checkoutRepo, empresaRepo, usuarioRepo,
                planRepo, new PasarelasPago(List.of(pasarela)), "http://frontend.test");
        CheckoutRequestDto solicitud = new CheckoutRequestDto("PRO", 1L, "admin@magnetix.test", "WOMPI",
                MetodoPago.BREB_QR, "3107654321", null, null, "CC", "1099888777", "Ana Perez",
                "ana@example.com", "3107654321", "Plan PRO", true);

        CheckoutResponseDto respuesta = servicioQr.iniciar(solicitud);

        assertEquals(CheckoutPago.TIPO_QR, respuesta.tipo());
        assertEquals("qr-1", respuesta.qrId());
        assertEquals("BASE64", respuesta.qrImage());
        assertEquals("tx-1", respuesta.transaccionId());
        assertEquals("wompi-qr:qr-1", respuesta.checkoutUrl());
        ArgumentCaptor<SolicitudPago> captor = ArgumentCaptor.forClass(SolicitudPago.class);
        verify(pasarela).crearCheckout(captor.capture());
        assertEquals(MetodoPago.BREB_QR, captor.getValue().metodo());
        assertEquals("ana@example.com", captor.getValue().payerEmail());
        assertEquals("CC", captor.getValue().documentType());
    }

    @Test
    void checkoutQrSinAceptacionDeTerminosFalla() {
        CheckoutRequestDto solicitud = new CheckoutRequestDto("PRO", 1L, "admin@magnetix.test", "WOMPI",
                MetodoPago.BREB_QR, null, null, null, null, null, null, null, null, null, false);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.iniciar(solicitud));

        assertTrue(error.getMessage().contains("términos"));
    }

    private static PasarelaWompi pasarelaWompi(String publicKey, String integritySecret) {
        return new PasarelaWompi(publicKey, integritySecret, "events-test",
                "https://checkout.test/p/", "", "https://api.test", 300, false, new ObjectMapper());
    }
}
