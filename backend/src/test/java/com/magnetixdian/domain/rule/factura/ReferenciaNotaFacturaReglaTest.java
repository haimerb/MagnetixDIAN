package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.factura.TipoDocumento;
import com.magnetixdian.domain.model.ResultadoRegla;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenciaNotaFacturaReglaTest {

    private final ReferenciaNotaFacturaRegla regla = new ReferenciaNotaFacturaRegla();

    @Test
    void facturaDeVentaNoRequiereReferencia() {
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(TestFacturas.base().build(), null));
        assertEquals(1, r.size());
        assertTrue(r.get(0).valida());
    }

    @Test
    void notaSinReferenciaReportaErrores() {
        var b = TestFacturas.base();
        b.tipo = TipoDocumento.NOTA_CREDITO;
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "numeroReferencia".equals(x.campo())));
        assertTrue(r.stream().anyMatch(x -> !x.valida() && "cufeReferencia".equals(x.campo())));
    }

    @Test
    void notaConReferenciaEsValida() {
        var b = TestFacturas.base();
        b.tipo = TipoDocumento.NOTA_DEBITO;
        b.numeroReferencia = "FV-1";
        b.cufeReferencia = "abc123";
        List<ResultadoRegla> r = regla.evaluar(new ContextoFactura(b.build(), null));
        assertEquals(1, r.size());
        assertTrue(r.get(0).valida());
    }
}
