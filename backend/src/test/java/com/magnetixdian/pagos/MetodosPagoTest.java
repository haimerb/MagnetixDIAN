package com.magnetixdian.pagos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.MetodoPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import com.magnetixdian.infrastructure.pago.PasarelaSimulada;
import com.magnetixdian.infrastructure.pago.PasarelaWompi;
import com.magnetixdian.interfaces.dto.CheckoutRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetodosPagoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private PasarelaWompi pasarela() {
        return new PasarelaWompi("pub_test", "integrity_test", "events_secret",
                "https://checkout.wompi.co/p/", "api_token", "https://api.wompi.co", 300, true, mapper);
    }

    private SolicitudPago solicitud(MetodoPago metodo, String celular) {
        return new SolicitudPago(1L, 2L, "WOMPI-123", "PRO", new BigDecimal("99000"), "COP",
                "Plan Profesional", "http://localhost/suscripcion", metodo, celular);
    }

    private SolicitudPago solicitudPSE(String banco) {
        return new SolicitudPago(1L, 2L, "WOMPI-123", "PRO", new BigDecimal("99000"), "COP",
                "Plan Profesional", "http://localhost/suscripcion", MetodoPago.PSE, null,
                banco, "0", "CC", "1099888777", "Ana Perez", "ana@example.com", "3107654321", "Plan PRO");
    }

    @Test
    void metodoPorDefectoEsCard() {
        SolicitudPago solicitud = new SolicitudPago(1L, 2L, "WOMPI-123", "PRO",
                new BigDecimal("99000"), "COP", "Plan Profesional", "http://localhost/suscripcion");
        CheckoutRequest request = new CheckoutRequest("PRO", "WOMPI");

        assertEquals(MetodoPago.CARD, solicitud.metodo());
        assertEquals(MetodoPago.CARD, request.metodo());

        CheckoutPago checkout = pasarela().crearCheckout(solicitud);
        assertTrue(checkout.url().contains("signature:integrity="));
        assertFalse(checkout.url().contains("payment_method="));
    }

    @Test
    void nequiSinCelularFalla() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> pasarela().crearCheckout(solicitud(MetodoPago.NEQUI, null)));
        assertTrue(error.getMessage().contains("celular"));
    }

    @Test
    void nequiConCelularGeneraCheckout() {
        CheckoutPago checkout = pasarela().crearCheckout(solicitud(MetodoPago.NEQUI, "3107654321"));
        assertTrue(checkout.url().contains("payment_method=NEQUI"));
        assertTrue(checkout.url().contains("celular=3107654321"));
        assertFalse(checkout.confirmadoInmediato());
    }

    @Test
    void pseSinBancoFalla() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> pasarela().crearCheckout(solicitudPSE(null)));
        assertTrue(error.getMessage().contains("financial_institution_id"));
    }

    @Test
    void pseConDatosGeneraCheckout() {
        CheckoutPago checkout = pasarela().crearCheckout(solicitudPSE("1"));
        assertTrue(checkout.url().contains("payment_method=PSE"));
        assertTrue(checkout.url().contains("financial_institution_id=1"));
        assertTrue(checkout.url().contains("document_number=1099888777"));
        assertTrue(checkout.url().contains("description=Plan+PRO"));
    }

    @Test
    void brebQrSinConfiguracionFalla() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> pasarela().crearCheckout(solicitud(MetodoPago.BREB_QR, null)));
        assertTrue(error.getMessage().contains("BREB_QR"));
        assertTrue(error.getMessage().contains("configurado"));
    }

    @Test
    void davipLataFallaControlada() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> pasarela().crearCheckout(solicitud(MetodoPago.DAVIPLATA, null)));
        assertTrue(error.getMessage().contains("próximamente"));
    }

    @Test
    void webhookConFirmaInvalidaSeRechaza() {
        String payload = """
                {
                  "data": { "transaction": { "reference": "WOMPI-123", "status": "APPROVED", "amount_in_cents": 9900000 } },
                  "timestamp": 1,
                  "signature": { "properties": ["transaction.reference"], "checksum": "invalido" }
                }
                """;
        Optional<com.magnetixdian.domain.pago.EventoPago> evento =
                pasarela().procesarWebhook(payload, Map.of());
        assertTrue(evento.isEmpty());
    }

    @Test
    void simuladaApruebaConMetodoDistintoDeCard() {
        CheckoutPago checkout = new PasarelaSimulada().crearCheckout(solicitud(MetodoPago.NEQUI, "3107654321"));
        assertTrue(checkout.confirmadoInmediato());
    }
}
