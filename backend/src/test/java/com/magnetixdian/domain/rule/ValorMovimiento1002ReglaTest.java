package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValorMovimiento1002ReglaTest {

    private final ValorMovimiento1002Regla regla = new ValorMovimiento1002Regla();

    @Test
    void soloAplicaAlFormato1002() {
        assertTrue(regla.aplicaA("1002"));
        assertFalse(regla.aplicaA("1001"));
    }

    @Test
    void movimientoPositivoNoReportaError() {
        assertTrue(regla.evaluar(contexto(operacion(new BigDecimal("1000"), null))).isEmpty());
    }

    @Test
    void notaCreditoPositivaSinMovimientoNoReportaError() {
        assertTrue(regla.evaluar(contexto(operacion(null, new BigDecimal("1000")))).isEmpty());
    }

    @Test
    void valoresCeroReportanError() {
        var resultado = regla.evaluar(contexto(operacion(BigDecimal.ZERO, BigDecimal.ZERO))).get(0);
        assertFalse(resultado.valida());
        assertEquals(Severidad.ERROR, resultado.severidad());
        assertEquals("valorPago", resultado.campo());
    }

    @Test
    void valoresNulosSeDejanAlaReglaDeObligatoriedad() {
        assertTrue(regla.evaluar(contexto(operacion(null, null))).isEmpty());
    }

    private OperacionDatos operacion(BigDecimal movimiento, BigDecimal notaCredito) {
        return new OperacionDatos(1, "NIT", "900123456", 8, "7001", movimiento,
                null, null, null, null, null, null, null, null, notaCredito, false);
    }

    private ContextoValidacion contexto(OperacionDatos op) {
        return new ContextoValidacion(op, new BigDecimal("52374"), Map.of(), null, "1002");
    }
}
