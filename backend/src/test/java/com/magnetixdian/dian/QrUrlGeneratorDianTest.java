package com.magnetixdian.dian;

import com.magnetixdian.domain.cufe.QrUrlGenerator;
import com.magnetixdian.domain.dian.ConfiguracionQrDian;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrUrlGeneratorDianTest {

    @Test
    void usaBaseConfiguradaDelAmbiente() {
        String url = QrUrlGenerator.generar("CUFE/1", 1,
                new ConfiguracionQrDian("https://qr.ejemplo.com/consulta?documentkey=",
                        "https://hab.ejemplo.com/consulta?documentkey=", "abc/123"));
        assertEquals("https://qr.ejemplo.com/consulta?documentkey=CUFE%2F1&hash=abc%2F123", url);
    }

    @Test
    void usaBasePorDefectoCuandoNoHayConfiguracion() {
        String url = QrUrlGenerator.generar("abc123", 2, new ConfiguracionQrDian(null, null, null));
        assertTrue(url.startsWith("https://catalogo-vpfe-hab.dian.gov.co/document/searchqr?documentkey="));
        assertTrue(url.endsWith("abc123"));
    }

    @Test
    void rechazaBaseNoHttps() {
        ConfiguracionQrDian configuracion = new ConfiguracionQrDian("http://inseguro.ejemplo.com", null, null);
        assertThrows(IllegalArgumentException.class,
                () -> QrUrlGenerator.generar("abc123", 1, configuracion));
    }
}
