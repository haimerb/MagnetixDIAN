package com.magnetixdian.domain.exogena;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XsdOficialesDian2025Test {

    private static final String BASE = "xsd/dian/2025/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void manifiestoDeclara69XsdOficiales() throws IOException {
        JsonNode manifest = manifest();
        assertEquals(69, manifest.get("total").asInt());
        assertEquals(manifest.get("total").asInt(), manifest.get("archivos").size());
    }

    @Test
    void todosLosXsdSonValidosYCoincidenConElManifiesto() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        JsonNode manifest = manifest();

        for (JsonNode item : manifest.get("archivos")) {
            String archivo = item.get("archivo").asText();
            ClassPathResource resource = resource(archivo);
            assertTrue(resource.exists(), archivo);
            assertEquals(item.get("formato").asText() + ".xsd", archivo);
            assertFalse(item.get("version").isNull(), archivo);
            assertFalse(item.get("encoding").isNull(), archivo);

            byte[] bytes = resource.getContentAsByteArray();
            assertEquals(item.get("bytes").asLong(), bytes.length, archivo);
            assertEquals(item.get("sha256").asText(), sha256(bytes), archivo);

            Schema schema = factory.newSchema(new StreamSource(new ByteArrayInputStream(bytes)));
            assertNotNull(schema, archivo);
        }
    }

    private JsonNode manifest() throws IOException {
        try (InputStream input = new ClassPathResource(BASE + "manifest.json").getInputStream()) {
            return objectMapper.readTree(input);
        }
    }

    private ClassPathResource resource(String archivo) {
        return new ClassPathResource(BASE + archivo);
    }

    private String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
