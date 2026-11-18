package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.factura.DatosAdquirente;
import com.magnetixdian.domain.factura.DatosEmisor;
import com.magnetixdian.domain.model.ResultadoRegla;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NitFacturaReglaTest {

    private final NitFacturaRegla regla = new NitFacturaRegla();

    @Test
    void dvCorrectoNoReportaErrores() {
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(TestFacturas.base().build(), null));
        assertEquals(1, r.size());
        assertTrue(r.get(0).valida());
    }

    @Test
    void dvIncorrectoDelEmisorReportaError() {
        var b = TestFacturas.base();
        b.emisor = new DatosEmisor("31", "900123456", 99, "Emisor", "O-13", "O-13",
                null, null, null, null, false);
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertEquals(1, r.size());
        assertEquals("emisor.digitoVerificacion", r.get(0).campo());
    }

    @Test
    void adquirenteNoNitNoSeValida() {
        var b = TestFacturas.base();
        b.adquirente = new DatosAdquirente("13", "123456", 99, "Persona", null,
                null, null, null, null);
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.get(0).valida());
    }
}
