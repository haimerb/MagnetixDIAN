package com.magnetixdian.domain.exogena;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapeadorExogenaOficialTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MapeadorExogenaOficial mapeador = new MapeadorExogenaOficial();

    @Test
    void mapeaAliasesYValoresNormalizados() throws IOException {
        JsonNode datos = fixture("1125");
        DocumentoExogena documento = mapeador.mapear("1125", datos, cabecera("1125", "13"));

        assertEquals("1125", documento.formato());
        assertEquals("13", documento.version());
        assertEquals(1, documento.codCpt());
        assertEquals(1, documento.numEnvio());
        assertEquals("1000000", documento.valorTotal());
        assertEquals(1, documento.cantReg());
        assertEquals("item", documento.elementoDetalle());
        assertEquals("900123456", documento.registros().get(0).atributos().get("nid"));
        assertEquals("PEREZ", documento.registros().get(0).atributos().get("apl1"));
        assertEquals("1000000", documento.registros().get(0).atributos().get("monto"));
        assertEquals("NO", documento.registros().get(0).atributos().get("reest"));
    }

    @Test
    void rechazaFilaSinCampoObligatorio() {
        ObjectNode fila = objectMapper.createObjectNode();
        fila.put("TIPO_DOCUMENTO", "31");
        ArrayNode filas = objectMapper.createArrayNode();
        filas.add(fila);
        ObjectNode datos = objectMapper.createObjectNode();
        datos.put("anioGravable", 2025);
        datos.set("filas", filas);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mapeador.mapear("1125", datos, cabecera("1125", "13")));

        assertTrue(error.getMessage().contains("nid"));
    }

    @Test
    void exponeEncabezadosYConceptosPorFormato() {
        assertTrue(mapeador.encabezados("5253").contains("VTRUPJ"));
        assertTrue(mapeador.encabezados("2278").contains("CLASEBN"));
        assertFalse(mapeador.encabezados("1125").isEmpty());
        assertEquals(1, mapeador.conceptos("1125").get(0).codCpt());
        assertEquals("BF", mapeador.conceptos("5253").get(0).codigo());
    }

    private CabeceraExogena cabecera(String formato, String version) {
        return new CabeceraExogena(formato, version, 2025, 1,
                "2025-01-15T10:00:00Z", "2025-01-01", "2025-12-31", null);
    }

    private JsonNode fixture(String formato) throws IOException {
        try (InputStream entrada = getClass().getResourceAsStream("/exogena/fixtures/" + formato + ".json")) {
            assertNotNull(entrada);
            return objectMapper.readTree(entrada);
        }
    }
}
