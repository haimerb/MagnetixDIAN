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
        assertTrue(registry.validarActivo("1001").activo());
        assertTrue(registry.validarActivo("1002").activo());
    }

    @Test
    void formatosPendientesNoSePuedenUsar() {
        FormatoExogena formato = registry.obtener("1003");
        assertFalse(formato.activo());
        assertEquals(FormatoExogenaRegistry.ESTADO_PENDIENTE_XSD, formato.estado());
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
        assertEquals(2, registry.activos().size());
    }
}
