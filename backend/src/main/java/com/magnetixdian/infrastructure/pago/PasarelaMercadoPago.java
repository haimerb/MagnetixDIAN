package com.magnetixdian.infrastructure.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.ConsultorPagoRemoto;
import com.magnetixdian.domain.pago.EventoPago;
import com.magnetixdian.domain.pago.PasarelaPago;
import com.magnetixdian.domain.pago.ProveedorPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import com.magnetixdian.domain.pago.ValidadorFirmaWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integración con MercadoPago (Checkout Pro). Requiere un access token.
 * Crea una preferencia y, ante una notificación, consulta el pago asociado.
 */
@Component
public class PasarelaMercadoPago implements PasarelaPago, ConsultorPagoRemoto {

    private static final Logger log = LoggerFactory.getLogger(PasarelaMercadoPago.class);
    public static final String NOMBRE = "MERCADOPAGO";

    private final String accessToken;
    private final String webhookSecret;
    private final String baseUrl;
    private final String backUrl;
    private final long webhookToleranceSeconds;
    private final boolean exigirFirmaWebhook;
    private final ObjectMapper mapper;
    private final RestClient http;
    private final ValidadorFirmaWebhook validador;

    public PasarelaMercadoPago(@Value("${magnetixdian.pagos.mercadopago.access-token:}") String accessToken,
                               @Value("${magnetixdian.pagos.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl,
                               @Value("${magnetixdian.pagos.mercadopago.back-url:}") String backUrl,
                               @Value("${magnetixdian.pagos.mercadopago.webhook-secret:}") String webhookSecret,
                               @Value("${magnetixdian.pagos.webhook-tolerance-seconds:300}") long webhookToleranceSeconds,
                               @Value("${magnetixdian.pagos.exigir-firma-webhook:false}") boolean exigirFirmaWebhook,
                               ObjectMapper mapper) {
        this.accessToken = accessToken;
        this.webhookSecret = webhookSecret;
        this.baseUrl = baseUrl;
        this.backUrl = backUrl;
        this.webhookToleranceSeconds = webhookToleranceSeconds;
        this.exigirFirmaWebhook = exigirFirmaWebhook;
        this.mapper = mapper;
        this.http = RestClient.builder().baseUrl(baseUrl).build();
        this.validador = new ValidadorFirmaWebhook("", webhookSecret, false, "", webhookToleranceSeconds, mapper);
    }

    @Override
    public String proveedor() {
        return NOMBRE;
    }

    @Override
    public boolean configurada() {
        return !accessToken.isBlank();
    }

    @Override
    public CheckoutPago crearCheckout(SolicitudPago solicitud) {
        if (!configurada()) {
            throw new IllegalStateException("La pasarela MercadoPago no está configurada.");
        }
        String retorno = solicitud.urlRetorno() != null ? solicitud.urlRetorno() : backUrl;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", solicitud.descripcion());
        item.put("quantity", 1);
        item.put("currency_id", solicitud.moneda() == null ? "COP" : solicitud.moneda());
        item.put("unit_price", solicitud.monto());

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("items", List.of(item));
        cuerpo.put("external_reference", solicitud.referencia());
        if (retorno != null && !retorno.isBlank()) {
            cuerpo.put("back_urls", Map.of("success", retorno, "pending", retorno, "failure", retorno));
        }

        JsonNode respuesta = http.post()
                .uri("/checkout/preferences")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(JsonNode.class);
        String url = respuesta == null ? null : respuesta.path("init_point").asText(null);
        if (url == null) {
            throw new IllegalStateException("MercadoPago no devolvió una URL de checkout.");
        }
        return CheckoutPago.redireccion(url);
    }

    @Override
    public Optional<EventoPago> procesarWebhook(String payload, Map<String, String> headers) {
        try {
            JsonNode raiz = mapper.readTree(payload);
            String pagoId = raiz.path("data").path("id").asText(null);
            if (pagoId == null || pagoId.isBlank()) {
                return Optional.empty();
            }
            if ((validador.configuradoPara(ProveedorPago.MERCADOPAGO) || validador.exigeFirma() || exigirFirmaWebhook)
                    && !validador.firmaVerificada(ProveedorPago.MERCADOPAGO, payload, headers)) {
                log.warn("Notificación de MercadoPago con firma inválida o ausente; se ignora.");
                return Optional.empty();
            }
            String tipo = raiz.path("type").asText(raiz.path("topic").asText(""));
            if (!"payment".equals(tipo)) {
                return Optional.empty();
            }
            JsonNode pago = http.get()
                    .uri("/v1/payments/{id}", pagoId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (pago == null) {
                return Optional.empty();
            }
            String referencia = pago.path("external_reference").asText(null);
            if (referencia == null) {
                return Optional.empty();
            }
            String estado = mapearEstado(pago.path("status").asText(""));
            BigDecimal monto = pago.path("transaction_amount").decimalValue();
            return Optional.of(new EventoPago(referencia, estado, monto));
        } catch (Exception e) {
            log.warn("No se pudo procesar la notificación de MercadoPago: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean configurado() {
        return configurada();
    }

    @Override
    public Optional<EventoPago> consultar(String referencia) {
        if (!configurada() || referencia == null || referencia.isBlank()) {
            return Optional.empty();
        }
        try {
            String referenciaCodificada = URLEncoder.encode(referencia, StandardCharsets.UTF_8);
            JsonNode respuesta = http.get()
                    .uri("/v1/payments/search?external_reference=" + referenciaCodificada
                            + "&sort=date_created&criteria=desc")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (respuesta == null) {
                return Optional.empty();
            }
            JsonNode resultados = respuesta.path("results");
            if (!resultados.isArray() || resultados.isEmpty()) {
                return Optional.empty();
            }
            JsonNode pago = resultados.get(0);
            String estado = mapearEstado(pago.path("status").asText(""));
            BigDecimal monto = pago.path("transaction_amount").decimalValue();
            return Optional.of(new EventoPago(referencia, estado, monto));
        } catch (Exception e) {
            log.warn("No se pudo consultar el pago {} en MercadoPago: {}", referencia, e.getMessage());
            return Optional.empty();
        }
    }

    private static String cabecera(Map<String, String> headers, String nombre) {
        if (headers == null) {
            return null;
        }
        String valor = headers.get(nombre);
        if (valor != null) {
            return valor;
        }
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(nombre))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    static boolean firmaValida(String xSignature, String xRequestId, String dataId, String secret) {
        return firmaValida(xSignature, xRequestId, dataId, secret, 300);
    }

    static boolean firmaValida(String xSignature, String xRequestId, String dataId, String secret,
                               long toleranciaSegundos) {
        if (secret == null || secret.isBlank()) {
            return false;
        }
        ValidadorFirmaWebhook validador = new ValidadorFirmaWebhook("", secret, false, "",
                toleranciaSegundos, new ObjectMapper());
        Map<String, String> headers = new LinkedHashMap<>();
        if (xSignature != null) {
            headers.put("x-signature", xSignature);
        }
        if (xRequestId != null) {
            headers.put("x-request-id", xRequestId);
        }
        if (dataId != null) {
            headers.put("data.id", dataId);
        }
        return validador.firmaVerificada(ProveedorPago.MERCADOPAGO, "{}", headers);
    }

    private static String mapearEstado(String estado) {
        return switch (estado) {
            case "approved" -> "APROBADO";
            case "rejected", "cancelled", "refunded", "charged_back" -> "RECHAZADO";
            default -> "PENDIENTE";
        };
    }
}
