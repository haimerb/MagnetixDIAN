package com.magnetixdian.domain.cufe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrUrlGeneratorTest {

    @Test
    void ambienteProduccionUsaCatalogoVpfe() {
        String url = QrUrlGenerator.generar("abc123", 1);
        assertTrue(url.startsWith("https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey="));
        assertTrue(url.endsWith("abc123"));
    }

    @Test
    void ambienteHabilitacionUsaCatalogoVpfeHab() {
        String url = QrUrlGenerator.generar("abc123", 2);
        assertTrue(url.startsWith("https://catalogo-vpfe-hab.dian.gov.co/document/searchqr?documentkey="));
    }

    @Test
    void cufeNuloNoRompeLaUrl() {
        assertEquals("https://catalogo-vpfe-hab.dian.gov.co/document/searchqr?documentkey=",
                QrUrlGenerator.generar(null, 2));
    }
}
