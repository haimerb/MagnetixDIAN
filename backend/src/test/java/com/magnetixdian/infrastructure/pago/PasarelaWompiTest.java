package com.magnetixdian.infrastructure.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.EventoPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasarelaWompiTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private PasarelaWompi pasarela(String eventsSecret) {
        return new PasarelaWompi("pub_test", "integrity_test", eventsSecret,
                "https://checkout.wompi.co/p/", "", "https://api.wompi.co", 300, false, mapper);
    }

    @Test
    void sinCredencialesNoEstaConfigurada() {
        PasarelaWompi sinClaves = new PasarelaWompi("", "", "", "https://checkout.wompi.co/p/",
                "", "https://api.wompi.co", 300, false, mapper);
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

    @Test
    void webhookConChecksumValidoSeNormaliza() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String checksum = sha256("WOMPI-123APPROVED" + timestamp + "events_secret");
        String payload = """
                {
                  "data": { "transaction": { "reference": "WOMPI-123", "status": "APPROVED", "amount_in_cents": 9900000 } },
                  "timestamp": %s,
                  "signature": { "properties": ["transaction.reference", "transaction.status"], "checksum": "%s" }
                }
                """.formatted(timestamp, checksum);

        Optional<EventoPago> evento = pasarela("events_secret").procesarWebhook(payload, Map.of());
        assertTrue(evento.isPresent());
        assertEquals("APROBADO", evento.get().estado());
    }

    @Test
    void webhookSinSecretoSeIgnoraCuandoLaFirmaEsObligatoria() {
        var pasarela = new PasarelaWompi("pub_test", "integrity_test", "", "https://checkout.wompi.co/p/",
                "", "https://api.wompi.co", 300, true, mapper);
        String payload = """
                {"data":{"transaction":{"reference":"WOMPI-123","status":"APPROVED","amount_in_cents":100}}}
                """;

        assertTrue(pasarela.procesarWebhook(payload, Map.of()).isEmpty());
    }

    private static String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
