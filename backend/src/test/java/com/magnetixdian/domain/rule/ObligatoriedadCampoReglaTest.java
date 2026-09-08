package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObligatoriedadCampoReglaTest {

    private final ObligatoriedadCampoRegla regla = new ObligatoriedadCampoRegla();

    @Test
    void camposCompletosNoGeneranResultados() {
        assertTrue(regla.evaluar(contexto(operacion("NIT", "900123456", "2012",
                new BigDecimal("3000000")))).isEmpty());
    }

    @Test
    void tipoDocumentoAusenteEsError() {
        ResultadoRegla r = regla.evaluar(contexto(operacion("", "900123456", "2012",
                new BigDecimal("3000000")))).get(0);
        assertTrue(!r.valida());
        assertEquals(Severidad.ERROR, r.severidad());
        assertEquals("tipoDocumento", r.campo());
    }

    @Test
    void nitAusenteEsError() {
        OperacionDatos op = operacion("NIT", "", "2012", new BigDecimal("3000000"));
        List<ResultadoRegla> resultados = regla.evaluar(contexto(op));
        assertTrue(resultados.stream().anyMatch(r -> !r.valida() && "numeroIdentificacion".equals(r.campo())));
    }

    @Test
    void conceptoAusenteEsError() {
        OperacionDatos op = operacion("NIT", "900123456", "", new BigDecimal("3000000"));
        List<ResultadoRegla> resultados = regla.evaluar(contexto(op));
        assertTrue(resultados.stream().anyMatch(r -> !r.valida() && "concepto".equals(r.campo())));
    }

    @Test
    void valorPagoAusenteEsError() {
        OperacionDatos op = operacion("NIT", "900123456", "2012", null);
        List<ResultadoRegla> resultados = regla.evaluar(contexto(op));
        assertTrue(resultados.stream().anyMatch(r -> !r.valida() && "valorPago".equals(r.campo())));
    }

    @Test
    void valorPagoNegativoEsError() {
        ResultadoRegla r = regla.evaluar(contexto(operacion("NIT", "900123456", "2012",
                new BigDecimal("-5")))).get(0);
        assertTrue(!r.valida());
        assertEquals("valorPago", r.campo());
    }

    private OperacionDatos operacion(String tipo, String nit, String concepto, BigDecimal pago) {
        return new OperacionDatos(1, tipo, nit, 8, concepto, pago,
                null, null, null, null, null, null, null, null, null, false);
    }

    private ContextoValidacion contexto(OperacionDatos op) {
        return new ContextoValidacion(op, new BigDecimal("52374"), java.util.Map.of(), null);
    }
}