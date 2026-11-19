package com.magnetixdian.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackoffTransmisionTest {

    @Test
    void aplicaBackoffCrecienteYSeMantieneEnElMaximo() {
        assertEquals(1, BackoffTransmision.minutos(0));
        assertEquals(1, BackoffTransmision.minutos(1));
        assertEquals(5, BackoffTransmision.minutos(2));
        assertEquals(15, BackoffTransmision.minutos(3));
        assertEquals(60, BackoffTransmision.minutos(4));
        assertEquals(240, BackoffTransmision.minutos(5));
        assertEquals(240, BackoffTransmision.minutos(99));
    }
}
