package com.magnetixdian.domain.contabilidad;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotorAsientosTest {

    @Test
    void validarAceptaAsientoBalanceado() {
        MotorAsientos.Resultado resultado = MotorAsientos.validar(List.of(
                MotorAsientos.linea("110505", new BigDecimal("100"), BigDecimal.ZERO),
                MotorAsientos.linea("420505", BigDecimal.ZERO, new BigDecimal("100"))));

        assertEquals(new BigDecimal("100.00"), resultado.totalDebito());
        assertEquals(new BigDecimal("100.00"), resultado.totalCredito());
        assertEquals(2, resultado.lineas().size());
    }

    @Test
    void validarRechazaAsientoDesbalanceado() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> MotorAsientos.validar(List.of(
                        MotorAsientos.linea("110505", new BigDecimal("100"), BigDecimal.ZERO),
                        MotorAsientos.linea("420505", BigDecimal.ZERO, new BigDecimal("90")))));

        assertTrue(error.getMessage().contains("balanceado"));
    }

    @Test
    void validarRechazaLineaConDebitoYCredito() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> MotorAsientos.validar(List.of(
                        MotorAsientos.linea("110505", new BigDecimal("100"), new BigDecimal("100")),
                        MotorAsientos.linea("420505", BigDecimal.ZERO, BigDecimal.ZERO))));

        assertTrue(error.getMessage().contains("débito y crédito"));
    }

    @Test
    void invertirIntercambiaValoresYMantieneCentroYTercero() {
        MotorAsientos.Resultado original = MotorAsientos.validar(List.of(
                new MotorAsientos.Linea("110505", "Cliente", new BigDecimal("1190"), BigDecimal.ZERO,
                        7L, 9L, new BigDecimal("1000"), "IVA"),
                new MotorAsientos.Linea("420505", "Venta", BigDecimal.ZERO, new BigDecimal("1190"),
                        null, null, null, null)));

        MotorAsientos.Resultado invertido = MotorAsientos.invertir(original, "Reversión");

        assertEquals(BigDecimal.ZERO.setScale(2), invertido.lineas().get(0).debito());
        assertEquals(new BigDecimal("1190.00"), invertido.lineas().get(0).credito());
        assertEquals(7L, invertido.lineas().get(0).centroCostoId());
        assertEquals(9L, invertido.lineas().get(0).terceroId());
        assertEquals(new BigDecimal("1000.00"), invertido.lineas().get(0).baseImponible());
        assertEquals("IVA", invertido.lineas().get(0).impuestoCodigo());
        assertEquals("Reversión", invertido.lineas().get(0).descripcion());
        assertEquals(new BigDecimal("1190.00"), invertido.lineas().get(1).debito());
        assertEquals(BigDecimal.ZERO.setScale(2), invertido.lineas().get(1).credito());
    }
}
