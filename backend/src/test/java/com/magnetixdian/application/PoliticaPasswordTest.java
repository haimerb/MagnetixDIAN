package com.magnetixdian.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaPasswordTest {

    @Test
    void aceptaPasswordConLetrasYNumeros() {
        assertDoesNotThrow(() -> PoliticaPassword.validar("Clave2026"));
    }

    @Test
    void rechazaPasswordCorta() {
        assertThrows(IllegalArgumentException.class, () -> PoliticaPassword.validar("Ab1"));
    }

    @Test
    void rechazaPasswordSinDigitos() {
        assertThrows(IllegalArgumentException.class, () -> PoliticaPassword.validar("sololetras"));
    }

    @Test
    void rechazaPasswordSinLetras() {
        assertThrows(IllegalArgumentException.class, () -> PoliticaPassword.validar("12345678"));
    }
}
