package com.magnetixdian.domain.exogena;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatoExogenaRegistryTest {

    private final FormatoExogenaRegistry registry = new FormatoExogenaRegistry();

    @Test
    void formatosLegacySiguenActivos() {
        FormatoExogena formato = registry.validarActivo("1001");
        assertTrue(formato.activo());
        assertEquals("10", formato.version());
        assertEquals("xsd/medio-magnetico-1001.xsd", formato.xsd());
        assertEquals("clasica", formato.validacion());
        assertEquals("GeneradorXml1001", formato.generadorXml());
        assertTrue(registry.validarActivo("1002").activo());
    }

    @Test
    void formatosOficiales1125_2278Y5253EstanActivos() {
        List<String> versiones = List.of("13", "1", "2");
        List<String> descripciones = List.of(
                "Declaración informativa individual de precios de transferencia",
                "Información de la compra de bonos electrónicos",
                "Información de beneficiarios efectivos");
        List<String> codigos = FormatoExogenaRegistry.FORMATOS_OFICIALES_ACTIVOS;
        for (int i = 0; i < codigos.size(); i++) {
            String codigo = codigos.get(i);
            FormatoExogena formato = registry.validarActivo(codigo);
            assertTrue(formato.activo());
            assertEquals(FormatoExogenaRegistry.ESTADO_ACTIVO, formato.estado());
            assertEquals(descripciones.get(i), formato.descripcion());
            assertEquals(versiones.get(i), formato.version());
            assertEquals("xsd/dian/2025/" + codigo + ".xsd", formato.xsd());
            assertEquals("xsd-local-2025", formato.validacion());
            assertEquals("GeneradorXmlExogenaOficial", formato.generadorXml());
            assertFalse(formato.encabezados().isEmpty());
        }
    }

    @Test
    void formatosConXsdOficialSiguenInactivos() {
        FormatoExogena formato = registry.obtener("1003");
        assertFalse(formato.activo());
        assertEquals(FormatoExogenaRegistry.ESTADO_PENDIENTE_GENERADOR, formato.estado());
        assertEquals("7", formato.version());
        assertEquals("xsd/dian/2025/1003.xsd", formato.xsd());
        assertThrows(IllegalArgumentException.class, () -> registry.validarActivo("1003"));
    }

    @Test
    void formatoDesconocidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> registry.validarActivo("9999"));
    }

    @Test
    void catalogoIncluyeFormatosSoportadosYPendientes() {
        List<String> codigos = registry.todos().stream().map(FormatoExogena::codigo).toList();
        assertTrue(codigos.contains("1001"));
        assertTrue(codigos.contains("1002"));
        assertTrue(codigos.contains("1003"));
        assertTrue(codigos.contains("1125"));
        assertTrue(codigos.contains("2278"));
        assertTrue(codigos.contains("5253"));
        assertEquals(5, registry.activos().size());
        assertEquals(73, registry.todos().size());
        assertEquals(68, registry.todos().stream()
                .filter(formato -> FormatoExogenaRegistry.ESTADO_PENDIENTE_GENERADOR.equals(formato.estado()))
                .count());
        assertEquals(0, registry.todos().stream()
                .filter(formato -> FormatoExogenaRegistry.ESTADO_PENDIENTE_XSD.equals(formato.estado()))
                .count());
    }
}
