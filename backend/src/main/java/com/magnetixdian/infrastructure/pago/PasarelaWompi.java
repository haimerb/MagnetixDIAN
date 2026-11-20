package com.magnetixdian.infrastructure.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.ConsultorPagoRemoto;
import com.magnetixdian.domain.pago.EventoPago;
import com.magnetixdian.domain.pago.PasarelaPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Integración con Wompi (Web Checkout). Requiere clave pública, secreto de
 * integridad y secreto de eventos. Verifica el checksum de los eventos.
 */
@Component
public class PasarelaWompi implements PasarelaPago, ConsultorPagoRemoto {

    private static final Logger log = LoggerFactory.getLogger(PasarelaWompi.class);
    public static final String NOMBRE = "WOMPI";

    private final String publicKey;
    private final String integritySecret;
    private final String eventsSecret;
    private final String checkoutUrl;
    private final String apiToken;
    private final long webhookToleranceSeconds;
    private final boolean exigirFirmaWebhook;
    private final ObjectMapper mapper;
    private final RestClient http;

    public PasarelaWompi(@Value("${magnetixdian.pagos.wompi.public-key:}") String publicKey,
                         @Value("${magnetixdian.pagos.wompi.integrity-secret:}") String integritySecret,
                         @Value("${magnetixdian.pagos.wompi.events-secret:}") String eventsSecret,
                         @Value("${magnetixdian.pagos.wompi.checkout-url:https://checkout.wompi.co/p/}") String checkoutUrl,
                         @Value("${magnetixdian.pagos.wompi.api-token:}") String apiToken,
                         @Value("${magnetixdian.pagos.wompi.api-url:https://api.wompi.co}") String apiUrl,
                         @Value("${magnetixdian.pagos.webhook-tolerance-seconds:300}") long webhookToleranceSeconds,
                         @Value("${magnetixdian.pagos.exigir-firma-webhook:false}") boolean exigirFirmaWebhook,
                         ObjectMapper mapper) {
        this.publicKey = publicKey;
        this.integritySecret = integritySecret;
        this.eventsSecret = eventsSecret;
        this.checkoutUrl = checkoutUrl;
        this.apiToken = apiToken;
        this.webhookToleranceSeconds = webhookToleranceSeconds;
        this.exigirFirmaWebhook = exigirFirmaWebhook;
        this.mapper = mapper;
        this.http = RestClient.builder().baseUrl(apiUrl).build();
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
                + "?public-key=" + parametro(publicKey)
                + "&currency=" + parametro(moneda)
                + "&amount-in-cents=" + centavos
                + "&reference=" + parametro(solicitud.referencia())
                + "&signature:integrity=" + parametro(integridad)
                + (solicitud.urlRetorno() != null && !solicitud.urlRetorno().isBlank()
                ? "&redirect-url=" + parametro(solicitud.urlRetorno()) : "");
        return CheckoutPago.redireccion(url);
    }

    @Override
    public boolean configurado() {
        return !apiToken.isBlank();
    }

    @Override
    public Optional<EventoPago> consultar(String referencia) {
        if (!configurado() || referencia == null || referencia.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode respuesta = http.get()
                    .uri("/v1/transactions?reference=" + parametro(referencia))
                    .header("Authorization", "Bearer " + apiToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (respuesta == null) {
                return Optional.empty();
            }
            JsonNode transaccion = respuesta.path("data");
            if (transaccion.isArray()) {
                if (transaccion.isEmpty()) {
                    return Optional.empty();
                }
                transaccion = transaccion.get(0);
            } else {
                transaccion = respuesta.path("transactions");
                if (transaccion.isArray()) {
                    if (transaccion.isEmpty()) {
                        return Optional.empty();
                    }
                    transaccion = transaccion.get(0);
                }
            }
            if (transaccion.isMissingNode() || transaccion.isNull()) {
                return Optional.empty();
            }
            String estado = mapearEstado(transaccion.path("status").asText(""));
            BigDecimal monto = BigDecimal.valueOf(transaccion.path("amount_in_cents").asLong(0))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return Optional.of(new EventoPago(referencia, estado, monto));
        } catch (Exception e) {
            log.warn("No se pudo consultar el pago {} en Wompi: {}", referencia, e.getMessage());
            return Optional.empty();
        }
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
            if (configurada() && exigirFirmaWebhook && eventsSecret.isBlank()) {
                log.warn("Evento Wompi sin secreto de firma; se ignora.");
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
        JsonNode properties = signature.path("properties");
        if (checksum.isBlank() || !properties.isArray() || properties.isEmpty()) {
            return false;
        }
        long marca;
        try {
            marca = Long.parseLong(raiz.path("timestamp").asText(""));
        } catch (NumberFormatException e) {
            return false;
        }
        if (marca <= 0 || Math.abs(Instant.now().getEpochSecond() - marca) > Math.max(0, webhookToleranceSeconds)) {
            return false;
        }
        StringBuilder concat = new StringBuilder();
        for (JsonNode propiedad : properties) {
            String valor = resolver(data, propiedad.asText(""));
            if (valor == null) {
                return false;
            }
            concat.append(valor);
        }
        concat.append(marca).append(eventsSecret);
        byte[] esperado = sha256(concat.toString()).getBytes(StandardCharsets.UTF_8);
        byte[] recibido = checksum.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperado, recibido);
    }

    private String resolver(JsonNode data, String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        JsonNode nodo = data;
        for (String parte : ruta.split("\\.")) {
            nodo = nodo.path(parte);
            if (nodo.isMissingNode() || nodo.isNull()) {
                return null;
            }
        }
        String valor = nodo.asText(null);
        return valor == null || valor.isBlank() ? null : valor;
    }

    private static String parametro(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
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
