package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptoValidoReglaTest {

    private final ConceptoValidoRegla regla = new ConceptoValidoRegla();

    private static final Map<String, String> CATALOGO = Map.of(
            "2012", "Pagos o abonos por compras",
            "2005", "Pagos o abonos por honorarios");

    @Test
    void conceptoDelCatalogoAcepta() {
        List<ResultadoRegla> resultados = regla.evaluar(contexto("2012"));
        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).valida());
    }

    @Test
    void conceptoDesconocidoReportaError() {
        List<ResultadoRegla> resultados = regla.evaluar(contexto("9999"));
        assertEquals(1, resultados.size());
        ResultadoRegla r = resultados.get(0);
        assertTrue(!r.valida());
        assertEquals(Severidad.ERROR, r.severidad());
        assertEquals("concepto", r.campo());
        assertEquals("9999", r.valorEncontrado());
    }

    @Test
    void conceptoVacioNoGeneraResultados() {
        assertTrue(regla.evaluar(contexto("")).isEmpty());
        assertTrue(regla.evaluar(contexto(null)).isEmpty());
    }

    @Test
    void ignoraEspaciosAlrededorDelConcepto() {
        assertTrue(regla.evaluar(contexto(" 2012 ")).get(0).valida());
    }

    private ContextoValidacion contexto(String concepto) {
        OperacionDatos op = new OperacionDatos(1, "NIT", "900123456", 8, concepto,
                new BigDecimal("3000000"), null, null, null, null,
                null, null, null, null, null, false);
        return new ContextoValidacion(op, new BigDecimal("52374"), CATALOGO, null);
    }
}