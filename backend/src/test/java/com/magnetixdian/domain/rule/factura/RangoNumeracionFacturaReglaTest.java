package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangoNumeracionFacturaReglaTest {

    private final RangoNumeracionFacturaRegla regla = new RangoNumeracionFacturaRegla();

    @Test
    void numeroDentroDeRangoYVigenteNoReportaErrores() {
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(TestFacturas.base().build(), null));
        assertEquals(1, r.size());
        assertTrue(r.get(0).valida());
    }

    @Test
    void numeroBajoElRangoReportaError() {
        var b = TestFacturas.base();
        b.rangoDesde = 10L;
        b.numero = 5L;
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "numero".equals(x.campo())));
    }

    @Test
    void numeroSobreElRangoReportaError() {
        var b = TestFacturas.base();
        b.rangoHasta = 100L;
        b.numero = 200L;
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "numero".equals(x.campo())));
    }

    @Test
    void fechaAntesDeLaVigenciaReportaError() {
        var b = TestFacturas.base();
        b.fechaEmision = LocalDate.of(2024, 1, 1);
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "fechaEmision".equals(x.campo())));
    }

    @Test
    void numeroCeroNoReportaErrorDeRango() {
        var b = TestFacturas.base();
        b.rangoDesde = 10L;
        b.numero = 0L;
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.get(0).valida());
    }

    @Test
    void sinResolucionReportaAdvertencia() {
        var b = TestFacturas.base();
        b.resolucionNumero = null;
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertEquals(1, r.size());
        assertEquals(Severidad.ADVERTENCIA, r.get(0).severidad());
    }
}
