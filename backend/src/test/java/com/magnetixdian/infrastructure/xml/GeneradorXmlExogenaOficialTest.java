package com.magnetixdian.infrastructure.xml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.exogena.CabeceraExogena;
import com.magnetixdian.domain.exogena.DocumentoExogena;
import com.magnetixdian.domain.exogena.MapeadorExogenaOficial;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorXmlExogenaOficialTest {

    private static final Map<String, String> VERSIONES = Map.of(
            "1125", "13",
            "2278", "1",
            "5253", "2");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MapeadorExogenaOficial mapeador = new MapeadorExogenaOficial();
    private final ValidadorXsdOficialDian2025 validador = new ValidadorXsdOficialDian2025();
    private final GeneradorXmlExogenaOficial generador = new GeneradorXmlExogenaOficial(validador);

    @Test
    void generaXmlValidoYCoincideConGolden() throws Exception {
        for (String formato : List.of("1125", "2278", "5253")) {
            String xml = generador.generar(documento(formato));
            assertTrue(xml.contains("<Formato>" + formato + "</Formato>"), formato);
            assertEquals(normalizar(golden(formato)), normalizar(xml), formato);
        }
    }

    @Test
    void validadorRechazaXmlQueNoCumpleEsquema() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar("<mas/>", "1125"));
    }

    private DocumentoExogena documento(String formato) throws IOException {
        try (InputStream entrada = getClass().getResourceAsStream("/exogena/fixtures/" + formato + ".json")) {
            assertNotNull(entrada);
            JsonNode datos = objectMapper.readTree(entrada);
            CabeceraExogena cabecera = new CabeceraExogena(
                    formato, VERSIONES.get(formato), 2025, 1,
                    "2025-01-15T10:00:00Z", "2025-01-01", "2025-12-31", null);
            return mapeador.mapear(formato, datos, cabecera);
        }
    }

    private String golden(String formato) throws IOException {
        try (InputStream entrada = getClass().getResourceAsStream("/exogena/golden/" + formato + ".xml")) {
            assertNotNull(entrada);
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String normalizar(String xml) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document documento = fabrica.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return canonico(documento.getDocumentElement());
    }

    private String canonico(Element elemento) {
        StringBuilder salida = new StringBuilder("<").append(elemento.getTagName());
        NamedNodeMap atributos = elemento.getAttributes();
        List<String> nombres = new ArrayList<>();
        for (int i = 0; i < atributos.getLength(); i++) {
            nombres.add(atributos.item(i).getNodeName());
        }
        Collections.sort(nombres);
        for (String nombre : nombres) {
            salida.append(' ').append(nombre).append("=\"")
                    .append(atributos.getNamedItem(nombre).getNodeValue()).append('"');
        }
        salida.append('>');
        NodeList hijos = elemento.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.ELEMENT_NODE) {
                salida.append(canonico((Element) hijo));
            } else if (hijo.getNodeType() == Node.TEXT_NODE && !hijo.getNodeValue().isBlank()) {
                salida.append(hijo.getNodeValue()
                        .replace("&", "&amp;")
                        .replace("<", "&lt;"));
            }
        }
        return salida.append("</").append(elemento.getTagName()).append('>').toString();
    }
}
