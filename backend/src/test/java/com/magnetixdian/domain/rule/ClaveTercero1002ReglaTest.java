package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaveTercero1002ReglaTest {

    private final ClaveTercero1002Regla regla = new ClaveTercero1002Regla();

    @Test
    void soloAplicaAlFormato1002() {
        assertTrue(regla.aplicaA("1002"));
        assertFalse(regla.aplicaA("1001"));
    }

    @Test
    void nitFormateadoConDvNoReportaError() {
        assertTrue(regla.evaluar(contexto(operacion("NIT", "900.123.456-8", 8))).isEmpty());
    }

    @Test
    void nitNoNumericoReportaError() {
        var resultado = regla.evaluar(contexto(operacion("NIT", "NIT-ABC", 8))).get(0);
        assertFalse(resultado.valida());
        assertEquals(Severidad.ERROR, resultado.severidad());
        assertEquals("numeroIdentificacion", resultado.campo());
    }

    @Test
    void nitSinDvReportaError() {
        var resultados = regla.evaluar(contexto(operacion("NITCE", "900123456", null)));
        assertTrue(resultados.stream().anyMatch(r -> !r.valida() && "dv".equals(r.campo())));
    }

    @Test
    void tipoDocumentoDiferenteNoEvalua() {
        assertTrue(regla.evaluar(contexto(operacion("CC", "ABC", null))).isEmpty());
    }

    private OperacionDatos operacion(String tipoDocumento, String numeroIdentificacion, Integer dv) {
        return new OperacionDatos(1, tipoDocumento, numeroIdentificacion, dv, "7001",
                new BigDecimal("1000"), null, null, null, null,
                null, null, null, null, null, false);
    }

    private ContextoValidacion contexto(OperacionDatos op) {
        return new ContextoValidacion(op, new BigDecimal("52374"), Map.of(), null, "1002");
    }
}
