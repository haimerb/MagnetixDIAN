package com.magnetixdian.infrastructure.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.EventoPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasarelaWompiTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private PasarelaWompi pasarela(String eventsSecret) {
        return new PasarelaWompi("pub_test", "integrity_test", eventsSecret,
                "https://checkout.wompi.co/p/", mapper);
    }

    @Test
    void sinCredencialesNoEstaConfigurada() {
        PasarelaWompi sinClaves = new PasarelaWompi("", "", "", "https://checkout.wompi.co/p/", mapper);
        assertFalse(sinClaves.configurada());
        assertTrue(pasarela("").configurada());
    }

    @Test
    void checkoutIncluyeMontoEnCentavosYFirma() {
        SolicitudPago solicitud = new SolicitudPago(1L, 2L, "WOMPI-123", "PRO",
                new BigDecimal("99000"), "COP", "Plan Profesional", "http://localhost/suscripcion");
        CheckoutPago checkout = pasarela("").crearCheckout(solicitud);
        assertFalse(checkout.confirmadoInmediato());
        assertTrue(checkout.url().contains("amount-in-cents=9900000"));
        assertTrue(checkout.url().contains("reference=WOMPI-123"));
        assertTrue(checkout.url().contains("signature:integrity="));
    }

    @Test
    void webhookAprobadoSeNormaliza() {
        String payload = """
                {
                  "event": "transaction.updated",
                  "data": { "transaction": { "reference": "WOMPI-123", "status": "APPROVED", "amount_in_cents": 9900000 } },
                  "timestamp": 1,
                  "signature": { "properties": [], "checksum": "" }
                }
                """;
        Optional<EventoPago> evento = pasarela("").procesarWebhook(payload, Map.of());
        assertTrue(evento.isPresent());
        assertEquals("WOMPI-123", evento.get().referencia());
        assertEquals("APROBADO", evento.get().estado());
        assertEquals(0, new BigDecimal("99000.00").compareTo(evento.get().monto()));
    }

    @Test
    void webhookConChecksumInvalidoSeIgnora() {
        String payload = """
                {
                  "data": { "transaction": { "reference": "WOMPI-123", "status": "APPROVED", "amount_in_cents": 1 } },
                  "timestamp": 1,
                  "signature": { "properties": ["transaction.reference"], "checksum": "incorrecto" }
                }
                """;
        assertTrue(pasarela("events_secret").procesarWebhook(payload, Map.of()).isEmpty());
    }
}
