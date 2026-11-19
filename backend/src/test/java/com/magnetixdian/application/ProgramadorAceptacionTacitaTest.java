package com.magnetixdian.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramadorAceptacionTacitaTest {

    @Test
    void restaDiasHabilesSaltaFinDeSemana() {
        // jueves 2019-05-02 menos 3 días hábiles -> lunes 2019-04-29
        assertEquals(LocalDate.of(2019, 4, 29),
                ProgramadorAceptacionTacita.restarDiasHabiles(LocalDate.of(2019, 5, 2), 3));
    }

    @Test
    void restaDiasHabilesDentroDeLaMismaSemana() {
        // viernes 2019-05-03 menos 2 días hábiles -> miércoles 2019-05-01
        assertEquals(LocalDate.of(2019, 5, 1),
                ProgramadorAceptacionTacita.restarDiasHabiles(LocalDate.of(2019, 5, 3), 2));
    }

    @Test
    void restaCeroDevuelveLaMismaFecha() {
        assertEquals(LocalDate.of(2019, 5, 3),
                ProgramadorAceptacionTacita.restarDiasHabiles(LocalDate.of(2019, 5, 3), 0));
    }
}
