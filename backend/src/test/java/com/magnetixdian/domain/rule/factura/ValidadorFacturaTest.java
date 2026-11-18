package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorFacturaTest {

    private final ValidadorFactura validador = new ValidadorFactura(List.of(
            new ObligatoriedadFacturaRegla(),
            new NitFacturaRegla(),
            new LineasFacturaRegla(),
            new TotalesFacturaRegla(),
            new RangoNumeracionFacturaRegla()));

    @Test
    void documentoValidoNoTieneErrores() {
        List<ResultadoRegla> r = validador.validar(new ContextoFactura(TestFacturas.base().build(), null));
        assertFalse(validador.tieneErrores(r));
    }

    @Test
    void documentoInvalidoTieneErrores() {
        var b = TestFacturas.base();
        b.fechaEmision = null;
        b.lineas = List.of();
        List<ResultadoRegla> r = validador.validar(new ContextoFactura(b.build(), null));
        assertTrue(validador.tieneErrores(r));
    }
}
