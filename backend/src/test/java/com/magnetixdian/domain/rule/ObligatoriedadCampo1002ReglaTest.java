package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObligatoriedadCampo1002ReglaTest {

    private final ObligatoriedadCampo1002Regla regla = new ObligatoriedadCampo1002Regla();

    private ContextoValidacion contexto(OperacionDatos op) {
        return new ContextoValidacion(op, new BigDecimal("52374"), Map.of(), null, "1002");
    }

    private OperacionDatos op(String concepto, BigDecimal valorPago, BigDecimal valorNc) {
        return new OperacionDatos(1, "NIT", "900123456", 8, concepto, valorPago,
                null, null, null, null, null, null, null, null, valorNc, false);
    }

    @Test
    void soloAplicaAlFormato1002() {
        assertTrue(regla.aplicaA("1002"));
        assertTrue(!regla.aplicaA("1001"));
    }

    @Test
    void registroCompletoConValorNotaCreditoNoReportaErrores() {
        List<ResultadoRegla> r = regla.evaluar(contexto(op("7001", null, new BigDecimal("500000"))));
        assertTrue(r.isEmpty());
    }

    @Test
    void registroCompletoConValorMovimientoNoReportaErrores() {
        List<ResultadoRegla> r = regla.evaluar(contexto(op("7005", new BigDecimal("500000"), null)));
        assertTrue(r.isEmpty());
    }

    @Test
    void sinConceptoReportaError() {
        List<ResultadoRegla> r = regla.evaluar(contexto(op(null, new BigDecimal("1000"), null)));
        assertEquals(1, r.size());
        assertEquals("concepto", r.get(0).campo());
    }

    @Test
    void sinValoresReportaError() {
        List<ResultadoRegla> r = regla.evaluar(contexto(op("7001", null, null)));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "valorPago".equals(x.campo())));
    }

    @Test
    void valorNotaCreditoNegativoReportaError() {
        List<ResultadoRegla> r = regla.evaluar(contexto(op("7001", null, new BigDecimal("-1"))));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "valorNc".equals(x.campo())));
    }
}
