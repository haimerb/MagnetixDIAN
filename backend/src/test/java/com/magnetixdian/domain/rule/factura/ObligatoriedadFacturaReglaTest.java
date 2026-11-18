package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObligatoriedadFacturaReglaTest {

    private final ObligatoriedadFacturaRegla regla = new ObligatoriedadFacturaRegla();

    @Test
    void documentoCompletoNoReportaErrores() {
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(TestFacturas.base().build(), null));
        assertEquals(1, r.size());
        assertTrue(r.get(0).valida());
    }

    @Test
    void sinFechaReportaError() {
        var b = TestFacturas.base();
        b.fechaEmision = null;
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "fechaEmision".equals(x.campo())));
    }

    @Test
    void sinEmisorReportaError() {
        var b = TestFacturas.base();
        b.emisor = null;
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "emisor.numeroIdentificacion".equals(x.campo())));
    }

    @Test
    void sinLineasReportaError() {
        var b = TestFacturas.base();
        b.lineas = List.of();
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "lineas".equals(x.campo())));
    }
}
