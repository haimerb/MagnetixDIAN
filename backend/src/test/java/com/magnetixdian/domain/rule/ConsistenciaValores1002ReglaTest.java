package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistenciaValores1002ReglaTest {

    private final ConsistenciaValores1002Regla regla = new ConsistenciaValores1002Regla();

    @Test
    void soloAplicaAlFormato1002() {
        assertTrue(regla.aplicaA("1002"));
        assertFalse(regla.aplicaA("1001"));
    }

    @Test
    void retencionQueSuperaElMovimientoEsError() {
        var resultado = regla.evaluar(contexto(operacion("1000000", null, "1500000", null, null))).get(0);
        assertFalse(resultado.valida());
        assertEquals(Severidad.ERROR, resultado.severidad());
        assertEquals("retencionRenta", resultado.campo());
    }

    @Test
    void retencionNegativaEsError() {
        var resultado = regla.evaluar(contexto(operacion("1000000", null, "-50", null, null))).get(0);
        assertFalse(resultado.valida());
        assertEquals("retencionRenta", resultado.campo());
    }

    @Test
    void sumaDeRetencionesQueSuperaElMovimientoEsError() {
        List<ResultadoRegla> resultados = regla.evaluar(contexto(
                operacion("1000000", null, "400000", "400000", "400000")));
        assertTrue(resultados.stream().anyMatch(r -> !r.valida() && "retenciones".equals(r.campo())));
    }

    @Test
    void valoresCoherentesNoGeneranResultados() {
        assertTrue(regla.evaluar(contexto(
                operacion("1000000", null, "100000", "50000", null))).isEmpty());
    }

    @Test
    void notaCreditoSirveComoBaseCuandoNoHayMovimiento() {
        var resultado = regla.evaluar(contexto(operacion(null, "500000", "600000", null, null))).get(0);
        assertFalse(resultado.valida());
        assertEquals("retencionRenta", resultado.campo());
    }

    @Test
    void sinBaseNoEvalua() {
        assertTrue(regla.evaluar(contexto(operacion(null, null, "100", null, null))).isEmpty());
    }

    private OperacionDatos operacion(String movimiento, String notaCredito,
                                     String renta, String iva, String ica) {
        return new OperacionDatos(1, "NIT", "900123456", 8, "7001", decimal(movimiento),
                decimal(renta), decimal(iva), decimal(ica), null,
                null, null, null, null, decimal(notaCredito), false);
    }

    private BigDecimal decimal(String valor) {
        return valor == null ? null : new BigDecimal(valor);
    }

    private ContextoValidacion contexto(OperacionDatos op) {
        return new ContextoValidacion(op, new BigDecimal("52374"), Map.of(), null, "1002");
    }
}
