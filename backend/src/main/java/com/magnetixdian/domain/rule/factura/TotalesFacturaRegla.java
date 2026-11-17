package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Regla FE-004: cuadre de totales (subtotal, impuestos, retenciones y total a
 * pagar) frente al detalle de las líneas.
 */
@Component
public class TotalesFacturaRegla implements ReglaFactura {

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "FE-004";
    }

    @Override
    public String nombre() {
        return "Cuadre de totales del documento";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoFactura ctx) {
        var f = ctx.factura();
        List<ResultadoRegla> r = new ArrayList<>();

        BigDecimal subtotalCalc = BigDecimal.ZERO;
        BigDecimal impuestosCalc = BigDecimal.ZERO;
        if (f.lineas() != null) {
            for (LineaFactura l : f.lineas()) {
                BigDecimal bruto = nz(l.cantidad()).multiply(nz(l.valorUnitario()));
                BigDecimal desc = nz(l.valorDescuento());
                subtotalCalc = subtotalCalc.add(bruto.subtract(desc));
                impuestosCalc = impuestosCalc.add(nz(l.ivaValor())).add(nz(l.incValor()));
            }
        }
        subtotalCalc = subtotalCalc.setScale(2, RoundingMode.HALF_UP);
        impuestosCalc = impuestosCalc.setScale(2, RoundingMode.HALF_UP);

        if (difiere(f.valorSubtotal(), subtotalCalc)) {
            r.add(err("subtotal", subtotalCalc, f.valorSubtotal()));
        }
        if (difiere(nz(f.totalImpuestos()), impuestosCalc)) {
            r.add(err("totalImpuestos", impuestosCalc, f.totalImpuestos()));
        }
        BigDecimal totalEsperado = subtotalCalc.add(impuestosCalc);
        if (difiere(nz(f.total()), totalEsperado)) {
            r.add(err("total", totalEsperado, f.total()));
        }
        BigDecimal pagoEsperado = nz(f.total()).subtract(nz(f.totalRetenciones())).setScale(2, RoundingMode.HALF_UP);
        if (difiere(nz(f.totalPagar()), pagoEsperado)) {
            r.add(err("totalPagar", pagoEsperado, f.totalPagar()));
        }
        return r.isEmpty() ? List.of(ResultadoRegla.ok(codigo())) : r;
    }

    private boolean difiere(BigDecimal a, BigDecimal b) {
        return nz(a).subtract(nz(b)).abs().compareTo(TOLERANCIA) > 0;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private ResultadoRegla err(String campo, BigDecimal esperado, BigDecimal encontrado) {
        return ResultadoRegla.error(codigo(), Severidad.ERROR,
                "El " + campo + " no cuadra con el detalle de las líneas",
                null, campo, esperado.toPlainString(), nz(encontrado).toPlainString());
    }
}
