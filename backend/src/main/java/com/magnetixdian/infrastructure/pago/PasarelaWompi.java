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
    private final String checkoutUrl;
    private final String apiToken;
    private final String brebQrUrl;
    private final String brebQrToken;
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
                         @Value("${magnetixdian.pagos.wompi.breb-qr-url:}") String brebQrUrl,
                         @Value("${magnetixdian.pagos.wompi.breb-qr-token:}") String brebQrToken,
                         ObjectMapper mapper) {
        this.publicKey = publicKey;
        this.integritySecret = integritySecret;
        this.checkoutUrl = checkoutUrl;
        this.apiToken = apiToken;
        this.brebQrUrl = brebQrUrl;
        this.brebQrToken = brebQrToken;
        this.exigirFirmaWebhook = exigirFirmaWebhook;
        this.mapper = mapper;
        this.http = RestClient.builder().baseUrl(apiUrl).build();
        this.validador = new ValidadorFirmaWebhook(eventsSecret, "", false, "", webhookToleranceSeconds, mapper);
    }

    public PasarelaWompi(String publicKey, String integritySecret, String eventsSecret, String checkoutUrl,
                         String apiToken, String apiUrl, long webhookToleranceSeconds, boolean exigirFirmaWebhook,
                         ObjectMapper mapper) {
        this(publicKey, integritySecret, eventsSecret, checkoutUrl, apiToken, apiUrl, webhookToleranceSeconds,
                exigirFirmaWebhook, "", "", mapper);
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
            case BREB_QR -> checkoutBrebQr(solicitud, centavos, moneda);
            case DAVIPLATA -> throw new IllegalStateException("DAVIPLATA estará disponible próximamente.");
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
            throw new IllegalArgumentException("Falta " + nombre + " para el pago PSE.");
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

    private CheckoutPago checkoutBrebQr(SolicitudPago solicitud, long centavos, String moneda) {
        if (brebQrUrl == null || brebQrUrl.isBlank() || brebQrToken == null || brebQrToken.isBlank()) {
            throw new IllegalStateException("BREB_QR no está configurado: faltan la URL y el token del QR de Wompi.");
        }
        Map<String, Object> metodoPago = new LinkedHashMap<>();
        metodoPago.put("type", "BANCOLOMBIA_QR");
        metodoPago.put("payment_description", descripcion(solicitud));
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("reference", solicitud.referencia());
        cuerpo.put("amount_in_cents", centavos);
        cuerpo.put("currency", moneda);
        cuerpo.put("payment_method", metodoPago);
        if (solicitud.urlRetorno() != null && !solicitud.urlRetorno().isBlank()) {
            cuerpo.put("redirect_url", solicitud.urlRetorno());
        }
        JsonNode respuesta;
        try {
            respuesta = http.post()
                    .uri(brebQrUrl)
                    .header("Authorization", "Bearer " + brebQrToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el QR BRE-B en Wompi: " + e.getMessage(), e);
        }
        String url = texto(respuesta, "/data/payment_method/extra/qr_url", "/data/qr_url", "/data/url", "/url");
        if (url != null && !url.isBlank()) {
            return CheckoutPago.redireccion(url);
        }
        String qrId = texto(respuesta, "/data/payment_method/extra/qr_id", "/data/qr_id", "/data/id", "/id");
        if (qrId != null && !qrId.isBlank()) {
            return CheckoutPago.redireccion("breb-qr:" + qrId);
        }
        throw new IllegalStateException("Wompi no devolvió una URL ni una referencia de QR para la transacción.");
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
