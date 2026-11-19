package com.magnetixdian.infrastructure.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.EventoPago;
import com.magnetixdian.domain.pago.PasarelaPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * Integración con Wompi (Web Checkout). Requiere clave pública, secreto de
 * integridad y secreto de eventos. Verifica el checksum de los eventos.
 */
@Component
public class PasarelaWompi implements PasarelaPago {

    private static final Logger log = LoggerFactory.getLogger(PasarelaWompi.class);
    public static final String NOMBRE = "WOMPI";

    private final String publicKey;
    private final String integritySecret;
    private final String eventsSecret;
    private final String checkoutUrl;
    private final ObjectMapper mapper;

    public PasarelaWompi(@Value("${magnetixdian.pagos.wompi.public-key:}") String publicKey,
                         @Value("${magnetixdian.pagos.wompi.integrity-secret:}") String integritySecret,
                         @Value("${magnetixdian.pagos.wompi.events-secret:}") String eventsSecret,
                         @Value("${magnetixdian.pagos.wompi.checkout-url:https://checkout.wompi.co/p/}") String checkoutUrl,
                         ObjectMapper mapper) {
        this.publicKey = publicKey;
        this.integritySecret = integritySecret;
        this.eventsSecret = eventsSecret;
        this.checkoutUrl = checkoutUrl;
        this.mapper = mapper;
    }

    @Override
    public String proveedor() {
        return NOMBRE;
    }

    @Override
    public boolean configurada() {
        return !publicKey.isBlank() && !integritySecret.isBlank();
    }

    @Override
    public CheckoutPago crearCheckout(SolicitudPago solicitud) {
        if (!configurada()) {
            throw new IllegalStateException("La pasarela Wompi no está configurada.");
        }
        long centavos = solicitud.monto().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
        String moneda = solicitud.moneda() == null ? "COP" : solicitud.moneda();
        String integridad = sha256(solicitud.referencia() + centavos + moneda + integritySecret);

        String url = checkoutUrl
                + "?public-key=" + publicKey
                + "&currency=" + moneda
                + "&amount-in-cents=" + centavos
                + "&reference=" + solicitud.referencia()
                + "&signature:integrity=" + integridad
                + (solicitud.urlRetorno() != null ? "&redirect-url=" + solicitud.urlRetorno() : "");
        return CheckoutPago.redireccion(url);
    }

    @Override
    public Optional<EventoPago> procesarWebhook(String payload, Map<String, String> headers) {
        try {
            JsonNode raiz = mapper.readTree(payload);
            JsonNode data = raiz.path("data");
            JsonNode transaccion = data.path("transaction");
            if (transaccion.isMissingNode()) {
                return Optional.empty();
            }
            if (!eventsSecret.isBlank() && !checksumValido(raiz, data)) {
                log.warn("Evento Wompi con checksum inválido; se ignora.");
                return Optional.empty();
            }
            String referencia = transaccion.path("reference").asText(null);
            String estado = mapearEstado(transaccion.path("status").asText(""));
            BigDecimal monto = BigDecimal.valueOf(transaccion.path("amount_in_cents").asLong(0))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (referencia == null) {
                return Optional.empty();
            }
            return Optional.of(new EventoPago(referencia, estado, monto));
        } catch (Exception e) {
            log.warn("No se pudo procesar el evento Wompi: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean checksumValido(JsonNode raiz, JsonNode data) {
        JsonNode signature = raiz.path("signature");
        String checksum = signature.path("checksum").asText("");
        StringBuilder concat = new StringBuilder();
        for (JsonNode propiedad : signature.path("properties")) {
            concat.append(resolver(data, propiedad.asText("")));
        }
        concat.append(raiz.path("timestamp").asLong(0)).append(eventsSecret);
        return sha256(concat.toString()).equalsIgnoreCase(checksum);
    }

    private String resolver(JsonNode data, String ruta) {
        JsonNode nodo = data;
        for (String parte : ruta.split("\\.")) {
            nodo = nodo.path(parte);
        }
        return nodo.asText("");
    }

    private static String mapearEstado(String estado) {
        return switch (estado) {
            case "APPROVED" -> "APROBADO";
            case "DECLINED", "VOIDED", "ERROR" -> "RECHAZADO";
            default -> "PENDIENTE";
        };
    }

    private static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
