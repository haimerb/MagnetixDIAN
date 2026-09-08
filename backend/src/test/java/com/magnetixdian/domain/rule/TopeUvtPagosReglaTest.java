package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopeUvtPagosReglaTest {

    private static final BigDecimal UVT = new BigDecimal("52374");
    private static final BigDecimal TOPE = UVT.multiply(new BigDecimal("3"));

    private final TopeUvtPagosRegla regla = new TopeUvtPagosRegla();

    @Test
    void pagoMenorALaCuantiaMenorGeneraAdvertencia() {
        OperacionDatos op = operacion(TOPE.subtract(BigDecimal.ONE), false);
        ResultadoRegla r = regla.evaluar(contexto(op)).get(0);
        assertTrue(!r.valida());
        assertEquals(Severidad.ADVERTENCIA, r.severidad());
        assertTrue(r.mensaje().contains("cuantía menor"));
    }

    @Test
    void pagoIgualAlTopeEsValido() {
        OperacionDatos op = operacion(TOPE, false);
        assertTrue(regla.evaluar(contexto(op)).get(0).valida());
    }

    @Test
    void cuantiaMenorMarcadaNoGeneraAdvertencia() {
        OperacionDatos op = operacion(BigDecimal.ONE, true);
        assertTrue(regla.evaluar(contexto(op)).get(0).valida());
    }

    @Test
    void usaCuantiaMenorExplicitaDelContexto() {
        BigDecimal cuantiaExplicita = new BigDecimal("150000");
        OperacionDatos op = operacion(new BigDecimal("149999"), false);
        ContextoValidacion contexto = new ContextoValidacion(op, UVT, java.util.Map.of(), cuantiaExplicita);
        ResultadoRegla r = regla.evaluar(contexto).get(0);
        assertTrue(!r.valida());
        assertEquals(Severidad.ADVERTENCIA, r.severidad());
    }

    @Test
    void sinValorDePagoNoEvalua() {
        OperacionDatos op = operacion(null, false);
        assertTrue(regla.evaluar(contexto(op)).isEmpty());
    }

    private OperacionDatos operacion(BigDecimal pago, boolean cuantiaMenor) {
        return new OperacionDatos(1, "NIT", "900123456", 8, "2012",
                pago, null, null, null, null,
                null, null, null, null, null, cuantiaMenor);
    }

    private ContextoValidacion contexto(OperacionDatos op) {
        return new ContextoValidacion(op, UVT, java.util.Map.of(), null);
    }
}