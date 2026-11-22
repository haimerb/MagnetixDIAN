package com.magnetixdian.infrastructure.importacion;

import com.magnetixdian.domain.integracion.MapeoColumnaDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MapperColumnasInteligente {

    public static final double UMBRAL_CONFIANZA = 0.35;
    private static final Pattern CODIGO_FORMATO = Pattern.compile("(^|\\D)(\\d{4})(\\D|$)");

    private static final Map<String, List<String>> ALIAS = new LinkedHashMap<>();
    private static final Map<String, String> CAMPOS_IMPORTADOR = new LinkedHashMap<>();

    static {
        ALIAS.put("razon_social", List.of("razon social", "razón social", "nombre", "nombre completo",
                "cliente", "adquirente", "adquirente nombre", "nombre razon social"));
        ALIAS.put("nit", List.of("nit", "documento", "numero documento", "numero de documento",
                "identificacion", "identificación", "cedula", "cédula", "nit cliente", "adquirente documento"));
        ALIAS.put("numero", List.of("numero", "número", "consecutivo", "numero factura",
                "numero de factura", "factura", "grupo"));
        ALIAS.put("fecha", List.of("fecha", "fecha factura", "fecha emision", "fecha de emision",
                "fecha expedicion", "fecha de expedicion"));
        ALIAS.put("subtotal", List.of("subtotal", "sub total", "base", "base gravable", "base imponible"));
        ALIAS.put("iva", List.of("iva", "impuestos", "impuesto", "valor iva", "iva valor"));
        ALIAS.put("retencion", List.of("retencion", "retención", "retenciones", "retencion fuente",
                "retención en la fuente"));
        ALIAS.put("total", List.of("total", "total factura", "valor total", "monto", "total a pagar"));
        ALIAS.put("valor_unitario", List.of("valor unitario", "valorunitario", "precio unitario",
                "precio", "valor", "vunitario"));
        ALIAS.put("descripcion", List.of("descripcion", "descripción", "producto", "detalle",
                "concepto", "item"));
        ALIAS.put("cantidad", List.of("cantidad", "unidades", "qty", "cant"));
        ALIAS.put("direccion", List.of("direccion", "dirección", "direccion cliente", "direccion adquirente"));
        ALIAS.put("email", List.of("email", "correo", "correo electronico", "correo electrónico",
                "correo electronico cliente"));
        ALIAS.put("telefono", List.of("telefono", "teléfono", "celular", "telefono cliente", "tel"));
        ALIAS.put("ciudad", List.of("ciudad", "municipio", "ciudad cliente", "ciudad adquirente"));

        CAMPOS_IMPORTADOR.put("razon_social", "adquirente_nombre");
        CAMPOS_IMPORTADOR.put("nit", "adquirente_numero_documento");
        CAMPOS_IMPORTADOR.put("numero", "grupo");
        CAMPOS_IMPORTADOR.put("direccion", "adquirente_direccion");
        CAMPOS_IMPORTADOR.put("email", "adquirente_email");
        CAMPOS_IMPORTADOR.put("ciudad", "adquirente_municipio");
    }

    public String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    public double puntuar(String cabecera, String campo) {
        if (cabecera == null || campo == null) {
            return 0;
        }
        List<String> alias = ALIAS.get(campo.trim().toLowerCase(Locale.ROOT));
        if (alias == null) {
            return 0;
        }
        String normalizada = normalizar(cabecera);
        if (normalizada.isEmpty()) {
            return 0;
        }
        List<String> tokensCabecera = tokens(cabecera);
        double mejor = 0;
        for (String valor : alias) {
            String aliasNormalizado = normalizar(valor);
            double puntuacion;
            if (normalizada.equals(aliasNormalizado)) {
                puntuacion = 1.0;
            } else if (!aliasNormalizado.isEmpty() && normalizada.startsWith(aliasNormalizado)) {
                puntuacion = 0.8;
            } else if (!aliasNormalizado.isEmpty() && normalizada.contains(aliasNormalizado)) {
                puntuacion = 0.6;
            } else if (!Collections.disjoint(tokensCabecera, tokens(valor))) {
                puntuacion = 0.4;
            } else {
                puntuacion = 0;
            }
            mejor = Math.max(mejor, puntuacion);
        }
        return mejor;
    }

    public List<MapeoColumnaDto> sugerir(List<String> cabeceras, String formatoDetectado) {
        List<MapeoColumnaDto> sugerencias = new ArrayList<>();
        if (cabeceras == null) {
            return sugerencias;
        }
        Map<String, MapeoColumnaDto> porCampo = new LinkedHashMap<>();
        for (int i = 0; i < cabeceras.size(); i++) {
            String cabecera = cabeceras.get(i);
            if (cabecera == null || cabecera.isBlank()) {
                continue;
            }
            String mejorCampo = null;
            double mejorPuntuacion = 0;
            for (String campo : ALIAS.keySet()) {
                double puntuacion = puntuar(cabecera, campo);
                if (puntuacion > mejorPuntuacion) {
                    mejorPuntuacion = puntuacion;
                    mejorCampo = campo;
                }
            }
            if (mejorCampo == null || mejorPuntuacion < UMBRAL_CONFIANZA) {
                continue;
            }
            MapeoColumnaDto actual = porCampo.get(mejorCampo);
            if (actual == null || mejorPuntuacion > actual.confianza()) {
                porCampo.put(mejorCampo,
                        new MapeoColumnaDto(mejorCampo, cabecera.trim(), i, mejorPuntuacion, formatoDetectado));
            }
        }
        sugerencias.addAll(porCampo.values());
        sugerencias.sort((a, b) -> Integer.compare(a.indice(), b.indice()));
        return sugerencias;
    }

    public String detectarFormato(String nombreHoja, List<String> cabeceras) {
        String codigo = extraerCodigo(nombreHoja);
        if (codigo == null && cabeceras != null) {
            for (String cabecera : cabeceras) {
                codigo = extraerCodigo(cabecera);
                if (codigo != null) {
                    break;
                }
            }
        }
        if (codigo != null) {
            return codigo;
        }
        if (cabeceras == null || cabeceras.isEmpty()) {
            return "VACIO";
        }
        StringBuilder firma = new StringBuilder();
        for (String cabecera : cabeceras) {
            firma.append(normalizar(cabecera)).append('|');
        }
        return "FORMATO-" + sha256(firma.toString()).substring(0, 12);
    }

    public Map<String, Integer> aCamposImportador(List<MapeoColumnaDto> columnas) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        if (columnas == null) {
            return resultado;
        }
        for (MapeoColumnaDto columna : columnas) {
            if (columna == null || columna.campo() == null || columna.campo().isBlank()
                    || columna.indice() < 0) {
                continue;
            }
            String campo = columna.campo().trim().toLowerCase(Locale.ROOT);
            resultado.put(CAMPOS_IMPORTADOR.getOrDefault(campo, campo), columna.indice());
        }
        return resultado;
    }

    private static String extraerCodigo(String valor) {
        if (valor == null) {
            return null;
        }
        Matcher matcher = CODIGO_FORMATO.matcher(valor);
        return matcher.find() ? matcher.group(2) : null;
    }

    private static List<String> tokens(String texto) {
        String base = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(base.split("[^a-z0-9]+"))
                .filter(token -> !token.isEmpty())
                .toList();
    }

    private static String sha256(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
