package com.magnetixdian.pago;

import com.magnetixdian.domain.pago.FirmaWebhookException;
import com.magnetixdian.domain.pago.ProveedorPago;
import com.magnetixdian.domain.pago.SecretoWebhookAusenteException;
import com.magnetixdian.domain.pago.ValidadorFirmaWebhook;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorFirmaWebhookBillingTest {

    @Test
    void aceptaFirmaHmacDelCuerpoCrudo() throws Exception {
        String payload = "{\"eventId\":\"evt-1\",\"monto\":99000}";
        String secreto = "secreto-wompi";
        String firma = hmac(payload, secreto);
        ValidadorFirmaWebhook validador = new ValidadorFirmaWebhook(secreto, "", false, "test");

        assertDoesNotThrow(() -> validador.verificar(ProveedorPago.WOMPI, payload,
                Map.of("X-Event-Signature", firma)));
        assertThrows(FirmaWebhookException.class, () -> validador.verificar(ProveedorPago.WOMPI, payload,
                Map.of("X-Event-Signature", "firma-invalida")));
    }

    @Test
    void produccionSinSecretoExigeConfiguracion() {
        ValidadorFirmaWebhook validador = new ValidadorFirmaWebhook("", "", false, "prod");

        assertThrows(SecretoWebhookAusenteException.class,
                () -> validador.verificar(ProveedorPago.WOMPI, "{}", Map.of()));
    }

    private static String hmac(String payload, String secreto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
