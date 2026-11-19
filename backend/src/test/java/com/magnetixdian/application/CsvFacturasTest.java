package com.magnetixdian.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvFacturasTest {

    @Test
    void separaCamposConPuntoYComa() {
        assertEquals("01;FE-1;100", CsvFacturas.fila("01", "FE-1", "100"));
    }

    @Test
    void escapaSeparadoresYComillas() {
        assertEquals("\"Razón; Social\"", CsvFacturas.escapar("Razón; Social"));
        assertEquals("\"Dice \"\"hola\"\"\"", CsvFacturas.escapar("Dice \"hola\""));
        assertEquals("", CsvFacturas.escapar(null));
    }
}
