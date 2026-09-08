package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NitDigitoVerificacionReglaTest {

    private final NitDigitoVerificacionRegla regla = new NitDigitoVerificacionRegla();

    @Test
    void calcularDvCasosConocidos() {
        assertEquals(8, NitDigitoVerificacionRegla.calcularDv("900123456"));
        assertEquals(6, NitDigitoVerificacionRegla.calcularDv("9"));
        assertEquals(1, NitDigitoVerificacionRegla.calcularDv("11"));
        assertEquals(0, NitDigitoVerificacionRegla.calcularDv("0"));
    }

    @Test
    void calcularDvValoresInvalidosDevuelveNull() {
        assertNull(NitDigitoVerificacionRegla.calcularDv(null));
        assertNull(NitDigitoVerificacionRegla.calcularDv("  "));
        assertNull(NitDigitoVerificacionRegla.calcularDv("900.123.456"));
        assertNull(NitDigitoVerificacionRegla.calcularDv("ABC"));
    }

    @Test
    void dvCorrectoAcepta() {
        OperacionDatos op = operacion("900123456", 8);
        List<ResultadoRegla> resultados = regla.evaluar(contexto(op));
        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).valida());
    }

    @Test
    void dvIncorrectoReportaErrorConEsperado() {
        OperacionDatos op = operacion("900123456", 3);
        List<ResultadoRegla> resultados = regla.evaluar(contexto(op));
        assertEquals(1, resultados.size());
        ResultadoRegla r = resultados.get(0);
        assertTrue(!r.valida());
        assertEquals(Severidad.ERROR, r.severidad());
        assertEquals("dv", r.campo());
        assertEquals("8", r.valorEsperado());
        assertEquals("3", r.valorEncontrado());
    }

    @Test
    void nitVacioNoGeneraResultados() {
        OperacionDatos op = operacion("", null);
        assertTrue(regla.evaluar(contexto(op)).isEmpty());
    }

    private ContextoValidacion contexto(OperacionDatos op) {
        return new ContextoValidacion(op, new BigDecimal("52374"), java.util.Map.of(), null);
    }

    private OperacionDatos operacion(String nit, Integer dv) {
        return new OperacionDatos(1, "NIT", nit, dv, "2012",
                new BigDecimal("3000000"), null, null, null, null,
                null, null, null, null, null, false);
    }
}