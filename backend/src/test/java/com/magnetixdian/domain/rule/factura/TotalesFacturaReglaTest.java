package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotalesFacturaReglaTest {

    private final TotalesFacturaRegla regla = new TotalesFacturaRegla();

    @Test
    void totalesQueCuadranNoReportanErrores() {
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(TestFacturas.base().build(), null));
        assertEquals(1, r.size());
        assertTrue(r.get(0).valida());
    }

    @Test
    void subtotalIncorrectoReportaError() {
        var b = TestFacturas.base();
        b.subtotal = new BigDecimal("999.00");
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "subtotal".equals(x.campo())));
    }

    @Test
    void totalIncorrectoReportaError() {
        var b = TestFacturas.base();
        b.total = new BigDecimal("1.00");
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "total".equals(x.campo())));
    }

    @Test
    void retencionesAjustanTotalAPagar() {
        var b = TestFacturas.base();
        b.totalRetenciones = new BigDecimal("5000.00");
        b.totalPagar = new BigDecimal("114000.00");
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.get(0).valida());
    }

    @Test
    void totalAPagarIncorrectoReportaError() {
        var b = TestFacturas.base();
        b.totalRetenciones = new BigDecimal("5000.00");
        b.totalPagar = new BigDecimal("119000.00");
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "totalPagar".equals(x.campo())));
    }
}
