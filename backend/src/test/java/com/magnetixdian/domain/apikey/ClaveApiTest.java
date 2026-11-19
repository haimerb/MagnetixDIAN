package com.magnetixdian.domain.apikey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaveApiTest {

    @Test
    void generarProduceClaveConPrefijoRecuperable() {
        ClaveApi.Generada generada = ClaveApi.generar();
        assertNotNull(generada.prefijo());
        assertEquals(8, generada.prefijo().length());
        assertTrue(generada.clave().startsWith(ClaveApi.MARCA));
        assertEquals(generada.prefijo(), ClaveApi.prefijoDe(generada.clave()));
        assertEquals(ClaveApi.MARCA.length() + 8 + 32, generada.clave().length());
    }

    @Test
    void clavesGeneradasSonDistintas() {
        assertFalse(ClaveApi.generar().clave().equals(ClaveApi.generar().clave()));
    }

    @Test
    void prefijoDeClaveInvalidaEsNulo() {
        assertNull(ClaveApi.prefijoDe(null));
        assertNull(ClaveApi.prefijoDe("sin-marca"));
        assertNull(ClaveApi.prefijoDe("mx_123"));
    }

    @Test
    void hashEsDeterministaYCoincide() {
        String clave = "mx_abcdef1234567890";
        String hash = ClaveApi.hash(clave);
        assertEquals(64, hash.length());
        assertEquals(hash, ClaveApi.hash(clave));
        assertTrue(ClaveApi.hashCoincide(hash, ClaveApi.hash(clave)));
        assertFalse(ClaveApi.hashCoincide(hash, ClaveApi.hash("otra")));
    }
}
