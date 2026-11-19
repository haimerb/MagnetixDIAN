package com.magnetixdian.domain.suscripcion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiteUsoTest {

    @Test
    void excedidoCuandoAlcanzaElLimite() {
        assertTrue(LimiteUso.excedido(50, 50));
        assertTrue(LimiteUso.excedido(51, 50));
    }

    @Test
    void noExcedidoPorDebajoDelLimite() {
        assertFalse(LimiteUso.excedido(49, 50));
    }

    @Test
    void limiteCeroEsIlimitado() {
        assertFalse(LimiteUso.excedido(9999, 0));
        assertNull(LimiteUso.restante(9999, 0));
    }

    @Test
    void restanteCalculaYNoBajaDeCero() {
        assertEquals(10, LimiteUso.restante(40, 50));
        assertEquals(0, LimiteUso.restante(55, 50));
    }
}
