package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineasFacturaReglaTest {

    private final LineasFacturaRegla regla = new LineasFacturaRegla();

    private List<ResultadoRegla> evaluar(List<LineaFactura> lineas) {
        var b = TestFacturas.base();
        b.lineas = lineas;
        return regla.evaluar(new ContextoFactura(b.build(), null));
    }

    @Test
    void lineaValidaNoReportaErrores() {
        List<ResultadoRegla> r = evaluar(List.of(TestFacturas.lineaValida()));
        assertEquals(1, r.size());
        assertTrue(r.get(0).valida());
    }

    @Test
    void descripcionVaciaReportaError() {
        LineaFactura l = LineaFactura.of(1, "", BigDecimal.ONE, new BigDecimal("1000"),
                BigDecimal.ZERO, BigDecimal.ZERO, "94", "80101500");
        List<ResultadoRegla> r = evaluar(List.of(l));
        assertEquals(1, r.size());
        assertEquals("descripcion", r.get(0).campo());
        assertEquals(Severidad.ERROR, r.get(0).severidad());
    }

    @Test
    void cantidadCeroReportaError() {
        LineaFactura l = LineaFactura.of(1, "Servicio", BigDecimal.ZERO, new BigDecimal("1000"),
                BigDecimal.ZERO, BigDecimal.ZERO, "94", "80101500");
        List<ResultadoRegla> r = evaluar(List.of(l));
        assertEquals(1, r.size());
        assertEquals("cantidad", r.get(0).campo());
    }

    @Test
    void valorUnitarioNegativoReportaError() {
        LineaFactura l = new LineaFactura(1, null, "Servicio", BigDecimal.ONE, "94",
                new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "80101500");
        List<ResultadoRegla> r = evaluar(List.of(l));
        assertEquals(1, r.size());
        assertEquals("valorUnitario", r.get(0).campo());
    }

    @Test
    void codigoUnspscObligatorio() {
        LineaFactura l = LineaFactura.of(1, "Servicio", BigDecimal.ONE, new BigDecimal("1000"),
                BigDecimal.ZERO, BigDecimal.ZERO, "94", null);
        List<ResultadoRegla> r = evaluar(List.of(l));
        assertEquals(1, r.size());
        assertEquals("codigoUnspsc", r.get(0).campo());
    }

    @Test
    void sinLineasNoReportaErrores() {
        assertTrue(evaluar(null).get(0).valida());
    }
}
