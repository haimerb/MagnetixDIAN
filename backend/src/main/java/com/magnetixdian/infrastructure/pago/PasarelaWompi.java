package com.magnetixdian.infrastructure.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.ConsultorPagoRemoto;
import com.magnetixdian.domain.pago.EventoPago;
import com.magnetixdian.domain.pago.MetodoPago;
import com.magnetixdian.domain.pago.PasarelaPago;
import com.magnetixdian.domain.pago.ProveedorPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import com.magnetixdian.domain.pago.ValidadorFirmaWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Integración con Wompi (Web Checkout, API de transacciones y webhooks).
 * Requiere clave pública, secreto de integridad, secreto de eventos y llave privada
 * para los métodos que se crean por API. Verifica la firma de los eventos.
 */
@Component
public class PasarelaWompi implements PasarelaPago, ConsultorPagoRemoto {

    private static final Logger log = LoggerFactory.getLogger(PasarelaWompi.class);
    public static final String NOMBRE = "WOMPI";

    private final String publicKey;
    private final String integritySecret;
    private final String checkoutUrl;
    private final String apiToken;
    private final int qrPollAttempts;
    private final long qrPollIntervalMs;
    private final String brebQrSandboxStatus;
    private final boolean exigirFirmaWebhook;
    private final ObjectMapper mapper;
    private final RestClient http;
    private final ValidadorFirmaWebhook validador;

    @Autowired
    public PasarelaWompi(@Value("${magnetixdian.pagos.wompi.public-key:}") String publicKey,
                         @Value("${magnetixdian.pagos.wompi.integrity-secret:}") String integritySecret,
                         @Value("${magnetixdian.pagos.wompi.events-secret:}") String eventsSecret,
                         @Value("${magnetixdian.pagos.wompi.checkout-url:https://checkout.wompi.co/p/}") String checkoutUrl,
                         @Value("${magnetixdian.pagos.wompi.api-token:}") String apiToken,
                         @Value("${magnetixdian.pagos.wompi.api-url:https://api.wompi.co}") String apiUrl,
                         @Value("${magnetixdian.pagos.webhook-tolerance-seconds:300}") long webhookToleranceSeconds,
                         @Value("${magnetixdian.pagos.exigir-firma-webhook:false}") boolean exigirFirmaWebhook,
                         @Value("${magnetixdian.pagos.wompi.qr-poll-attempts:10}") int qrPollAttempts,
                         @Value("${magnetixdian.pagos.wompi.qr-poll-interval-ms:500}") long qrPollIntervalMs,
                         @Value("${magnetixdian.pagos.wompi.breb-qr-sandbox-status:APPROVED}") String brebQrSandboxStatus,
                         ObjectMapper mapper) {
        this(publicKey, integritySecret, eventsSecret, checkoutUrl, apiToken, apiUrl, webhookToleranceSeconds,
                exigirFirmaWebhook, qrPollAttempts, qrPollIntervalMs, brebQrSandboxStatus, mapper,
                RestClient.builder().baseUrl(apiUrl).build());
    }

    public PasarelaWompi(String publicKey, String integritySecret, String eventsSecret, String checkoutUrl,
                         String apiToken, String apiUrl, long webhookToleranceSeconds, boolean exigirFirmaWebhook,
                         ObjectMapper mapper) {
        this(publicKey, integritySecret, eventsSecret, checkoutUrl, apiToken, apiUrl, webhookToleranceSeconds,
                exigirFirmaWebhook, 10, 500, "APPROVED", mapper,
                RestClient.builder().baseUrl(apiUrl).build());
    }

    PasarelaWompi(String publicKey, String integritySecret, String eventsSecret, String checkoutUrl,
                  String apiToken, String apiUrl, long webhookToleranceSeconds, boolean exigirFirmaWebhook,
                  int qrPollAttempts, long qrPollIntervalMs, String brebQrSandboxStatus,
                  ObjectMapper mapper, RestClient http) {
        this.publicKey = publicKey;
        this.integritySecret = integritySecret;
        this.checkoutUrl = checkoutUrl;
        this.apiToken = apiToken;
        this.qrPollAttempts = qrPollAttempts;
        this.qrPollIntervalMs = qrPollIntervalMs;
        this.brebQrSandboxStatus = brebQrSandboxStatus;
        this.exigirFirmaWebhook = exigirFirmaWebhook;
        this.mapper = mapper;
        this.http = http;
        this.validador = new ValidadorFirmaWebhook(eventsSecret, "", false, "", webhookToleranceSeconds, mapper);
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
        String url = urlCheckout(solicitud, centavos, moneda);
        MetodoPago metodo = solicitud.metodo() == null ? MetodoPago.CARD : solicitud.metodo();
        String celular = metodo == MetodoPago.NEQUI ? celularNequi(solicitud) : null;
        return switch (metodo) {
            case CARD -> CheckoutPago.redireccion(url);
            case NEQUI -> CheckoutPago.redireccion(url + "&payment_method=NEQUI&payment-method=NEQUI"
                    + "&celular=" + parametro(celular)
                    + "&customer-data:phone-number=" + parametro(celular)
                    + "&customer-data:phone-number-prefix=%2B57");
            case PSE -> CheckoutPago.redireccion(url + "&payment_method=PSE&payment-method=PSE"
                    + urlPse(solicitud));
            case BREB_QR, DAVIPLATA -> crearTransaccionApi(solicitud, centavos, moneda, metodo);
        };
    }

    private String urlCheckout(SolicitudPago solicitud, long centavos, String moneda) {
        String integridad = sha256(solicitud.referencia() + centavos + moneda + integritySecret);
        return checkoutUrl
                + "?public-key=" + parametro(publicKey)
                + "&currency=" + parametro(moneda)
                + "&amount-in-cents=" + centavos
                + "&reference=" + parametro(solicitud.referencia())
                + "&signature:integrity=" + parametro(integridad)
                + (solicitud.urlRetorno() != null && !solicitud.urlRetorno().isBlank()
                ? "&redirect-url=" + parametro(solicitud.urlRetorno()) : "");
    }

    private static String celularNequi(SolicitudPago solicitud) {
        String celular = solicitud.celular() == null ? "" : solicitud.celular().replaceAll("[\\s()-]", "");
        if (!celular.matches("\\d{10}")) {
            throw new IllegalArgumentException("El celular de Nequi es obligatorio y debe tener 10 dígitos.");
        }
        return celular;
    }

    private static String urlPse(SolicitudPago solicitud) {
        String descripcion = descripcion(solicitud);
        return "&financial_institution_id=" + parametro(requerido(solicitud.financialInstitutionId(), "financial_institution_id"))
                + "&payment_type=" + parametro(requerido(solicitud.paymentType(), "payment_type"))
                + "&document_type=" + parametro(requerido(solicitud.documentType(), "document_type"))
                + "&document_number=" + parametro(requerido(solicitud.documentNumber(), "document_number"))
                + "&payer_name=" + parametro(requerido(solicitud.payerName(), "payer_name"))
                + "&payer_email=" + parametro(requerido(solicitud.payerEmail(), "payer_email"))
                + "&payer_phone=" + parametro(requerido(solicitud.payerPhone(), "payer_phone"))
                + "&description=" + parametro(descripcion)
                + "&customer-data:full-name=" + parametro(requerido(solicitud.payerName(), "payer_name"))
                + "&customer-data:email=" + parametro(requerido(solicitud.payerEmail(), "payer_email"))
                + "&customer-data:legal-id=" + parametro(requerido(solicitud.documentNumber(), "document_number"))
                + "&customer-data:legal-id-type=" + parametro(requerido(solicitud.documentType(), "document_type"));
    }

    private static String requerido(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta " + nombre + " para el pago.");
        }
        return valor.trim();
    }

    private static String descripcion(SolicitudPago solicitud) {
        String valor = solicitud.descripcionPago() == null || solicitud.descripcionPago().isBlank()
                ? solicitud.descripcion() : solicitud.descripcionPago();
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La descripción del pago es obligatoria.");
        }
        if (valor.length() > 64) {
            throw new IllegalArgumentException("La descripción del pago no puede superar 64 caracteres.");
        }
        return valor;
    }

    private static String descripcion(SolicitudPago solicitud, int maximo) {
        String valor = descripcion(solicitud);
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private CheckoutPago crearTransaccionApi(SolicitudPago solicitud, long centavos, String moneda,
                                             MetodoPago metodo) {
        if (apiToken.isBlank()) {
            throw new IllegalStateException("Wompi requiere la llave privada (api-token) para " + metodo + ".");
        }
        TokensAceptacion tokens = tokensAceptacion();
        String tipo = metodo == MetodoPago.BREB_QR ? "BANCOLOMBIA_QR" : "DAVIPLATA";
        Map<String, Object> metodoPago = new LinkedHashMap<>();
        metodoPago.put("type", tipo);
        metodoPago.put("payment_description", descripcion(solicitud, metodo == MetodoPago.DAVIPLATA ? 30 : 64));
        if (metodo == MetodoPago.BREB_QR && sandbox()) {
            metodoPago.put("sandbox_status", brebQrSandboxStatus.trim().toUpperCase(Locale.ROOT));
        }
        if (metodo == MetodoPago.DAVIPLATA) {
            metodoPago.put("user_legal_id_type", requerido(solicitud.documentType(), "document_type"));
            metodoPago.put("user_legal_id", requerido(solicitud.documentNumber(), "document_number"));
        }
        if (solicitud.urlRetorno() != null && !solicitud.urlRetorno().isBlank()) {
            metodoPago.put("redirect_url", solicitud.urlRetorno());
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("acceptance_token", tokens.aceptacion());
        cuerpo.put("accept_personal_auth", tokens.datosPersonales());
        cuerpo.put("amount_in_cents", centavos);
        cuerpo.put("currency", moneda);
        cuerpo.put("customer_email", requerido(solicitud.payerEmail(), "customer_email"));
        cuerpo.put("reference", requerido(solicitud.referencia(), "reference"));
        if (metodo == MetodoPago.DAVIPLATA) {
            cuerpo.put("payment_method_type", tipo);
        }
        cuerpo.put("payment_method", metodoPago);
        cuerpo.put("signature", sha256(solicitud.referencia() + centavos + moneda + integritySecret));
        if (solicitud.urlRetorno() != null && !solicitud.urlRetorno().isBlank()) {
            cuerpo.put("redirect_url", solicitud.urlRetorno());
        }

        JsonNode respuesta;
        try {
            respuesta = http.post()
                    .uri("/v1/transactions")
                    .header("Authorization", "Bearer " + apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear la transacción " + metodo + " en Wompi: "
                    + e.getMessage(), e);
        }
        String transaccionId = requerido(texto(respuesta, "/data/id"), "id de transacción");
        if (metodo == MetodoPago.BREB_QR) {
            JsonNode transaccion = esperarTransaccion(transaccionId, "QR",
                    "/data/payment_method/extra/qr_image", "/data/payment_method/qr_image",
                    "/data/payment_method/extra/qr_id", "/data/payment_method/qr_id");
            String qrImage = texto(transaccion, "/data/payment_method/extra/qr_image",
                    "/data/payment_method/qr_image");
            String qrId = texto(transaccion, "/data/payment_method/extra/qr_id",
                    "/data/payment_method/qr_id");
            return CheckoutPago.qr(qrId == null ? transaccionId : qrId, qrImage, transaccionId);
        }
        JsonNode transaccion = esperarTransaccion(transaccionId, "OTP",
                "/data/payment_method/extra/url", "/data/payment_method/url", "/data/url");
        String urlOtp = texto(transaccion, "/data/payment_method/extra/url",
                "/data/payment_method/url", "/data/url");
        return CheckoutPago.otp(urlOtp, transaccionId);
    }

    private TokensAceptacion tokensAceptacion() {
        JsonNode respuesta;
        try {
            respuesta = http.get()
                    .uri("/merchants/info")
                    .header("x-merchant-public-key", publicKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudieron obtener los tokens de aceptación de Wompi: "
                    + e.getMessage(), e);
        }
        String aceptacion = texto(respuesta, "/data/presigned_acceptance/acceptance_token");
        String datosPersonales = texto(respuesta, "/data/presigned_personal_data_auth/acceptance_token");
        if (aceptacion == null || datosPersonales == null) {
            throw new IllegalStateException("Wompi no devolvió los tokens de aceptación requeridos.");
        }
        return new TokensAceptacion(aceptacion, datosPersonales);
    }

    private JsonNode esperarTransaccion(String transaccionId, String nombre, String... rutas) {
        int intentos = Math.max(1, qrPollAttempts);
        JsonNode ultima = null;
        for (int intento = 0; intento < intentos; intento++) {
            ultima = consultarTransaccion(transaccionId);
            for (String ruta : rutas) {
                String valor = texto(ultima, ruta);
                if (valor != null && !valor.isBlank()) {
                    return ultima;
                }
            }
            if (intento < intentos - 1 && qrPollIntervalMs > 0) {
                try {
                    Thread.sleep(qrPollIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Se interrumpió la espera del " + nombre + " en Wompi.", e);
                }
            }
        }
        throw new IllegalStateException("Wompi no devolvió el " + nombre + " de la transacción " + transaccionId + ".");
    }

    private JsonNode consultarTransaccion(String transaccionId) {
        try {
            return http.get()
                    .uri("/v1/transactions/" + parametro(transaccionId))
                    .header("Authorization", "Bearer " + apiToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo consultar la transacción " + transaccionId
                    + " en Wompi: " + e.getMessage(), e);
        }
    }

    private record TokensAceptacion(String aceptacion, String datosPersonales) {
    }

    private boolean sandbox() {
        return publicKey.startsWith("pub_test_") || apiToken.startsWith("prv_test_");
    }

    private static String texto(JsonNode respuesta, String... rutas) {
        if (respuesta == null) {
            return null;
        }
        for (String ruta : rutas) {
            String valor = respuesta.at(ruta).asText(null);
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
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
            if ((validador.configuradoPara(ProveedorPago.WOMPI) || validador.exigeFirma() || exigirFirmaWebhook)
                    && !validador.firmaVerificada(ProveedorPago.WOMPI, payload, headers)) {
                log.warn("Evento Wompi con firma inválida o ausente; se ignora.");
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
