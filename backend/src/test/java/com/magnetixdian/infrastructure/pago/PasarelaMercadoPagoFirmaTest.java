package com.magnetixdian.infrastructure.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasarelaMercadoPagoFirmaTest {

    private static final String SECRETO = "webhook-secret";

    @Test
    void aceptaFirmaValidaConRequestId() {
        String ts = tsAhora();
        String requestId = "req-123";
        String dataId = "123456";
        String firma = firmar("id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";");

        assertTrue(PasarelaMercadoPago.firmaValida("ts=" + ts + ",v1=" + firma, requestId, dataId, SECRETO));
    }

    @Test
    void aceptaFirmaSinRequestId() {
        String ts = tsAhora();
        String dataId = "123456";
        String firma = firmar("id:" + dataId + ";ts:" + ts + ";");

        assertTrue(PasarelaMercadoPago.firmaValida("ts=" + ts + ",v1=" + firma, null, dataId, SECRETO));
    }

    @Test
    void aceptaClavesDeFirmaEnMayusculas() {
        String ts = tsAhora();
        String dataId = "123456";
        String firma = firmar("id:" + dataId + ";ts:" + ts + ";");

        assertTrue(PasarelaMercadoPago.firmaValida("TS=" + ts + ",V1=" + firma.toUpperCase(), null, dataId, SECRETO));
    }

    @Test
    void rechazaFirmaFueraDeVentana() {
        String ts = String.valueOf(Instant.now().getEpochSecond() - 901);
        String dataId = "123456";
        String firma = firmar("id:" + dataId + ";ts:" + ts + ";");

        assertFalse(PasarelaMercadoPago.firmaValida("ts=" + ts + ",v1=" + firma, null, dataId, SECRETO));
    }

    @Test
    void rechazaFirmaAlterada() {
        String ts = tsAhora();
        String firma = firmar("id:123456;ts:" + ts + ";");

        assertFalse(PasarelaMercadoPago.firmaValida("ts=" + ts + ",v1=" + firma, null, "999", SECRETO));
    }

    @Test
    void rechazaNotificacionSinFirma() {
        assertFalse(PasarelaMercadoPago.firmaValida(null, "req-1", "123456", SECRETO));
        assertFalse(PasarelaMercadoPago.firmaValida("ts=1700000000", "req-1", "123456", SECRETO));
    }

    @Test
    void sinSecretoConfiguradoNoValida() {
        assertTrue(PasarelaMercadoPago.firmaValida(null, null, null, ""));
        assertTrue(PasarelaMercadoPago.firmaValida("ts=1,v1=bad", null, "123456", null));
    }

    @Test
    void webhookConFirmaInvalidaSeIgnoraSinConsultarMercadoPago() {
        var pasarela = new PasarelaMercadoPago("token", "http://localhost:1", "", SECRETO, 300, false, new ObjectMapper());
        String payload = """
                {"type":"payment","data":{"id":"123456"}}
                """;
        Map<String, String> headers = Map.of(
                "x-signature", "ts=1700000000,v1=invalida",
                "x-request-id", "req-1",
                "data.id", "123456"
        );

        assertTrue(pasarela.procesarWebhook(payload, headers).isEmpty());
    }

    @Test
    void webhookSinSecretoSeIgnoraCuandoLaFirmaEsObligatoria() {
        var pasarela = new PasarelaMercadoPago("token", "http://localhost:1", "", "", 300, true, new ObjectMapper());
        String payload = """
                {"type":"payment","data":{"id":"123456"}}
                """;

        assertTrue(pasarela.procesarWebhook(payload, Map.of()).isEmpty());
    }

    private static String tsAhora() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    private static String firmar(String manifiesto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
