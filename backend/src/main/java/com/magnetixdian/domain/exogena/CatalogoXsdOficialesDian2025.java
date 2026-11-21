package com.magnetixdian.domain.exogena;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CatalogoXsdOficialesDian2025 {

    private static final String MANIFIESTO = "xsd/dian/2025/manifest.json";
    private static final Map<String, String> VERSIONES = cargar();

    private CatalogoXsdOficialesDian2025() {
    }

    public static Map<String, String> versiones() {
        return VERSIONES;
    }

    private static Map<String, String> cargar() {
        try (InputStream input = new ClassPathResource(MANIFIESTO).getInputStream()) {
            JsonNode root = new ObjectMapper().readTree(input);
            Map<String, String> versiones = new LinkedHashMap<>();
            for (JsonNode item : root.get("archivos")) {
                JsonNode version = item.get("version");
                versiones.put(
                        item.get("formato").asText(),
                        version == null || version.isNull() ? null : version.asText()
                );
            }
            return Collections.unmodifiableMap(versiones);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el manifiesto de XSD oficiales DIAN 2025", e);
        }
    }
}
