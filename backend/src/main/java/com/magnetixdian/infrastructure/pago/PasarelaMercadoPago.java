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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integración con MercadoPago (Checkout Pro). Requiere un access token.
 * Crea una preferencia y, ante una notificación, consulta el pago asociado.
 */
@Component
public class PasarelaMercadoPago implements PasarelaPago {

    private static final Logger log = LoggerFactory.getLogger(PasarelaMercadoPago.class);
    public static final String NOMBRE = "MERCADOPAGO";

    private final String accessToken;
    private final String baseUrl;
    private final String backUrl;
    private final ObjectMapper mapper;
    private final RestClient http;

    public PasarelaMercadoPago(@Value("${magnetixdian.pagos.mercadopago.access-token:}") String accessToken,
                               @Value("${magnetixdian.pagos.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl,
                               @Value("${magnetixdian.pagos.mercadopago.back-url:}") String backUrl,
                               ObjectMapper mapper) {
        this.accessToken = accessToken;
        this.baseUrl = baseUrl;
        this.backUrl = backUrl;
        this.mapper = mapper;
        this.http = RestClient.builder().baseUrl(baseUrl).build();
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
            String tipo = raiz.path("type").asText(raiz.path("topic").asText(""));
            if (!"payment".equals(tipo)) {
                return Optional.empty();
            }
            String pagoId = raiz.path("data").path("id").asText(null);
            if (pagoId == null) {
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

    private static String mapearEstado(String estado) {
        return switch (estado) {
            case "approved" -> "APROBADO";
            case "rejected", "cancelled", "refunded", "charged_back" -> "RECHAZADO";
            default -> "PENDIENTE";
        };
    }
}
