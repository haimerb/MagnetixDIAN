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
    void formatosPendientesNoSePuedenUsar() {
        FormatoExogena formato = registry.obtener("1125");
        assertFalse(formato.activo());
        assertEquals(FormatoExogenaRegistry.ESTADO_PENDIENTE_XSD, formato.estado());
        assertThrows(IllegalArgumentException.class, () -> registry.validarActivo("1125"));
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
        assertEquals(2, registry.activos().size());
        assertEquals(68, registry.todos().stream()
                .filter(formato -> FormatoExogenaRegistry.ESTADO_PENDIENTE_GENERADOR.equals(formato.estado()))
                .count());
        assertEquals(3, registry.todos().stream()
                .filter(formato -> FormatoExogenaRegistry.ESTADO_PENDIENTE_XSD.equals(formato.estado()))
                .count());
    }
}
