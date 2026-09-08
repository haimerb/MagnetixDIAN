package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistenciaValoresReglaTest {

    private final ConsistenciaValoresRegla regla = new ConsistenciaValoresRegla();

    @Test
    void retencionQueSuperaPagoEsError() {
        OperacionDatos op = operacion(new BigDecimal("1000000"), new BigDecimal("1500000"), null, null);
        ResultadoRegla r = regla.evaluar(contexto(op)).get(0);
        assertTrue(!r.valida());
        assertEquals(Severidad.ERROR, r.severidad());
        assertEquals("retencionRenta", r.campo());
    }

    @Test
    void retencionNegativaEsError() {
        OperacionDatos op = operacion(new BigDecimal("1000000"), new BigDecimal("-50"), null, null);
        ResultadoRegla r = regla.evaluar(contexto(op)).get(0);
        assertTrue(!r.valida());
        assertEquals(Severidad.ERROR, r.severidad());
    }

    @Test
    void sumaDeRetencionesQueSuperaElPagoEsError() {
        OperacionDatos op = operacion(new BigDecimal("1000000"),
                new BigDecimal("400000"), new BigDecimal("400000"), new BigDecimal("400000"));
        List<ResultadoRegla> resultados = regla.evaluar(contexto(op));
        assertTrue(resultados.stream().anyMatch(r -> !r.valida()
                && "retenciones".equals(r.campo())));
    }

    @Test
    void valoresCoherentesNoGeneranResultados() {
        OperacionDatos op = operacion(new BigDecimal("1000000"),
                new BigDecimal("100000"), new BigDecimal("50000"), null);
        assertTrue(regla.evaluar(contexto(op)).isEmpty());
    }

    @Test
    void retencionesNulasNoCausanError() {
        OperacionDatos op = operacion(new BigDecimal("1000000"), null, null, null);
        assertTrue(regla.evaluar(contexto(op)).isEmpty());
    }

    @Test
    void sinValorDePagoNoEvalua() {
        OperacionDatos op = operacion(null, new BigDecimal("100"), null, null);
        assertTrue(regla.evaluar(contexto(op)).isEmpty());
    }

    private OperacionDatos operacion(BigDecimal pago, BigDecimal renta, BigDecimal iva, BigDecimal ica) {
        return new OperacionDatos(1, "NIT", "900123456", 8, "2012",
                pago, renta, iva, ica, null,
                null, null, null, null, null, false);
    }

    private ContextoValidacion contexto(OperacionDatos op) {
        return new ContextoValidacion(op, new BigDecimal("52374"), java.util.Map.of(), null);
    }
}