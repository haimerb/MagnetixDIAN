package com.magnetixdian.domain.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ValidadorFirmaWebhook {

    private static final Pattern PATRON_ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final String ALGORITMO = "HmacSHA256";

    private final String secretoWompi;
    private final String secretoMercadoPago;
    private final boolean failFast;
    private final boolean produccion;
    private final long toleranciaSegundos;
    private final ObjectMapper mapper;

    @Autowired
    public ValidadorFirmaWebhook(
            @Value("${magnetixdian.pagos.wompi.events-secret:${WOMPI_EVENTS_SECRET:}}") String secretoWompi,
            @Value("${magnetixdian.pagos.mercadopago.webhook-secret:${MERCADOPAGO_WEBHOOK_SECRET:}}") String secretoMercadoPago,
            @Value("${magnetixdian.seguridad.fail-fast:${SEGURIDAD_FAIL_FAST:false}}") boolean failFast,
            @Value("${spring.profiles.active:${SPRING_PROFILES_ACTIVE:}}") String perfil,
            @Value("${magnetixdian.pagos.webhook-tolerance-seconds:300}") long toleranciaSegundos,
            ObjectMapper mapper) {
        this.secretoWompi = limpiar(secretoWompi);
        this.secretoMercadoPago = limpiar(secretoMercadoPago);
        this.failFast = failFast;
        this.produccion = esPerfilProduccion(perfil);
        this.toleranciaSegundos = Math.max(0, toleranciaSegundos);
        this.mapper = mapper;
    }

    public ValidadorFirmaWebhook(String secretoWompi, String secretoMercadoPago, boolean failFast, String perfil) {
        this(secretoWompi, secretoMercadoPago, failFast, perfil, 300, new ObjectMapper());
    }

    public boolean esProduccion() {
        return produccion;
    }

    public boolean exigeFirma() {
        return failFast || produccion;
    }

    public void verificar(ProveedorPago proveedor, String payload, Map<String, String> headers) {
        String secreto = secreto(proveedor);
        if (secreto.isEmpty()) {
            if (exigeFirma()) {
                throw new SecretoWebhookAusenteException("Secreto de webhook no configurado para " + proveedor);
            }
            return;
        }
        String cabecera = cabeceraFirma(headers);
        if (cabecera == null || cabecera.isBlank()) {
            if (proveedor == ProveedorPago.WOMPI && checksumWompiValido(payload, secreto)) {
                return;
            }
            throw new FirmaWebhookException("Firma del webhook ausente para " + proveedor);
        }
        if (!firmaValida(proveedor, payload, cabecera, headers, secreto)) {
            throw new FirmaWebhookException("Firma del webhook inválida para " + proveedor);
        }
    }

    public boolean firmaVerificada(ProveedorPago proveedor, String payload, Map<String, String> headers) {
        String secreto = secreto(proveedor);
        if (secreto.isEmpty()) {
            return false;
        }
        String cabecera = cabeceraFirma(headers);
        if (cabecera != null && !cabecera.isBlank()) {
            return firmaValida(proveedor, payload, cabecera, headers, secreto);
        }
        return proveedor == ProveedorPago.WOMPI && checksumWompiValido(payload, secreto);
    }

    private static String cabeceraFirma(Map<String, String> headers) {
        String cabecera = cabecera(headers, "X-Event-Signature");
        return cabecera == null || cabecera.isBlank() ? cabecera(headers, "X-Signature") : cabecera;
    }

    public boolean firmaValida(String payload, String cabecera, String secreto) {
        return firmaValida(ProveedorPago.WOMPI, payload, cabecera, Map.of(), secreto);
    }

    private boolean firmaValida(ProveedorPago proveedor, String payload, String cabecera,
                                Map<String, String> headers, String secreto) {
        if (proveedor == ProveedorPago.MERCADOPAGO && esFirmaMercadoPago(cabecera)) {
            return firmaMercadoPagoValida(cabecera, headers, payload, secreto);
        }
        String recibido = limpiarFirma(cabecera);
        if (recibido.isEmpty()) {
            return false;
        }
        String esperado = hmacHex(payload, secreto);
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }

    private boolean firmaMercadoPagoValida(String cabecera, Map<String, String> headers,
                                           String payload, String secreto) {
        String ts = null;
        String v1 = null;
        for (String parte : cabecera.split(",")) {
            String[] par = parte.split("=", 2);
            if (par.length != 2) {
                continue;
            }
            String clave = par[0].trim().toLowerCase(Locale.ROOT);
            if ("ts".equals(clave)) {
                ts = par[1].trim();
            } else if ("v1".equals(clave)) {
                v1 = limpiarFirma(par[1]);
            }
        }
        if (ts == null || v1 == null || v1.isEmpty()) {
            return false;
        }
        long marca;
        try {
            marca = Long.parseLong(ts);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(Instant.now().getEpochSecond() - marca) > toleranciaSegundos) {
            return false;
        }
        String id = idDelPayload(payload);
        if (id == null || id.isBlank()) {
            id = cabecera(headers, "data.id");
        }
        if (id == null || id.isBlank()) {
            return false;
        }
        StringBuilder manifiesto = new StringBuilder("id:").append(id).append(';');
        String requestId = cabecera(headers, "X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            manifiesto.append("request-id:").append(requestId).append(';');
        }
        manifiesto.append("ts:").append(ts).append(';');
        return MessageDigest.isEqual(hmacHex(manifiesto.toString(), secreto).getBytes(StandardCharsets.UTF_8),
                v1.getBytes(StandardCharsets.UTF_8));
    }

    private boolean checksumWompiValido(String payload, String secreto) {
        try {
            JsonNode raiz = mapper.readTree(payload);
            String checksum = raiz.path("signature").path("checksum").asText("");
            JsonNode propiedades = raiz.path("signature").path("properties");
            long marca = Long.parseLong(raiz.path("timestamp").asText(""));
            if (checksum.isBlank() || !propiedades.isArray() || propiedades.isEmpty()
                    || Math.abs(Instant.now().getEpochSecond() - marca) > toleranciaSegundos) {
                return false;
            }
            StringBuilder concat = new StringBuilder();
            for (JsonNode propiedad : propiedades) {
                String valor = raiz.at("/data/" + propiedad.asText("").replace('.', '/')).asText(null);
                if (valor == null || valor.isBlank()) {
                    return false;
                }
                concat.append(valor);
            }
            concat.append(marca).append(secreto);
            byte[] esperado = sha256Hex(concat.toString()).getBytes(StandardCharsets.UTF_8);
            byte[] recibido = checksum.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(esperado, recibido);
        } catch (Exception e) {
            return false;
        }
    }

    private String secreto(ProveedorPago proveedor) {
        return switch (proveedor) {
            case WOMPI -> secretoWompi;
            case MERCADOPAGO -> secretoMercadoPago;
            case SIMULADO -> "";
        };
    }

    private static boolean esFirmaMercadoPago(String cabecera) {
        String valor = cabecera.toLowerCase(Locale.ROOT);
        return valor.contains("ts=") && valor.contains("v1=");
    }

    private static String idDelPayload(String payload) {
        Matcher matcher = PATRON_ID.matcher(payload == null ? "" : payload);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String limpiarFirma(String valor) {
        String resultado = valor == null ? "" : valor.trim();
        while (resultado.startsWith("0x") || resultado.startsWith("0X")) {
            resultado = resultado.substring(2);
        }
        if (resultado.regionMatches(true, 0, "sha256=", 0, 7)) {
            resultado = resultado.substring(7);
        }
        if (resultado.regionMatches(true, 0, "v1=", 0, 3)) {
            resultado = resultado.substring(3);
        }
        return resultado.trim().toLowerCase(Locale.ROOT);
    }

    private static String cabecera(Map<String, String> headers, String nombre) {
        if (headers == null || headers.isEmpty()) {
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

    private static String hmacHex(String manifiesto, String secreto) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return HexFormat.of().formatHex(mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new FirmaWebhookException("No se pudo calcular la firma HMAC-SHA256.");
        }
    }

    private static String sha256Hex(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new FirmaWebhookException("SHA-256 no disponible.");
        }
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static boolean esPerfilProduccion(String perfil) {
        if (perfil == null || perfil.isBlank()) {
            return false;
        }
        return Arrays.stream(perfil.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(p -> "prod".equals(p) || "production".equals(p));
    }
}
