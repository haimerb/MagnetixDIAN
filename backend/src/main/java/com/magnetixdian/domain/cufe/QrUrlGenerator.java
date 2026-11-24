package com.magnetixdian.domain.cufe;

import com.magnetixdian.domain.dian.AmbienteDian;
import com.magnetixdian.domain.dian.ConfiguracionQrDian;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class QrUrlGenerator {

    private static final String RECURSO = "/wsdl/dian/qr-defaults.properties";
    private static final Properties BASES = cargarBases();

    private QrUrlGenerator() {
    }

    public static String generar(String cufe, int ambiente) {
        return generar(cufe, ambiente, new ConfiguracionQrDian(null, null, null));
    }

    public static String generar(String cufe, int ambiente, ConfiguracionQrDian configuracion) {
        AmbienteDian ambienteDian = AmbienteDian.desde(ambiente);
        String configurada = ambienteDian == AmbienteDian.PRODUCCION
                ? configuracion == null ? null : configuracion.baseUrlProduccion()
                : configuracion == null ? null : configuracion.baseUrlHabilitacion();
        String base = configurada == null || configurada.isBlank()
                ? BASES.getProperty(ambienteDian == AmbienteDian.PRODUCCION ? "produccion" : "habilitacion")
                : configurada;
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("No hay URL base configurada para el QR DIAN del ambiente " + ambiente);
        }
        StringBuilder url = new StringBuilder(normalizarBase(base));
        url.append(URLEncoder.encode(cufe == null ? "" : cufe, StandardCharsets.UTF_8));
        String hash = configuracion == null ? null : configuracion.hash();
        if (hash != null && !hash.isBlank()) {
            url.append("&hash=").append(URLEncoder.encode(hash, StandardCharsets.UTF_8));
        }
        return url.toString();
    }

    private static String normalizarBase(String base) {
        String valor = base.trim();
        URI uri = URI.create(valor);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("La URL base del QR DIAN debe ser HTTPS");
        }
        int parametro = valor.indexOf("?documentkey=");
        if (parametro >= 0) {
            return valor.substring(0, parametro) + "?documentkey=";
        }
        if (valor.contains("?")) {
            return valor.endsWith("?") || valor.endsWith("&") ? valor + "documentkey=" : valor + "&documentkey=";
        }
        return valor + "?documentkey=";
    }

    private static Properties cargarBases() {
        Properties propiedades = new Properties();
        try (InputStream entrada = QrUrlGenerator.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró la configuración base del QR DIAN: " + RECURSO);
            }
            propiedades.load(entrada);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la configuración base del QR DIAN", e);
        }
        return propiedades;
    }
}
