package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Regla FE-003: cada línea debe tener descripción, cantidad y valor unitario
 * positivos, y un código de producto (UNSPSC) válido.
 */
@Component
public class LineasFacturaRegla implements ReglaFactura {

    @Override
    public String codigo() {
        return "FE-003";
    }

    @Override
    public String nombre() {
        return "Integridad de las líneas del documento";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoFactura ctx) {
        List<ResultadoRegla> r = new ArrayList<>();
        var lineas = ctx.factura().lineas();
        if (lineas == null) {
            return List.of(ResultadoRegla.ok(codigo()));
        }
        int i = 1;
        for (LineaFactura l : lineas) {
            if (l.descripcion() == null || l.descripcion().isBlank()) {
                r.add(err(i, "descripcion", "La descripción de la línea es obligatoria", null));
            }
            if (l.cantidad() == null || l.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                r.add(err(i, "cantidad", "La cantidad debe ser mayor a cero", l.cantidad()));
            }
            if (l.valorUnitario() == null || l.valorUnitario().compareTo(BigDecimal.ZERO) < 0) {
                r.add(err(i, "valorUnitario", "El valor unitario no puede ser negativo", l.valorUnitario()));
            }
            if (l.codigoUnspsc() == null || l.codigoUnspsc().isBlank()) {
                r.add(err(i, "codigoUnspsc", "El código de producto (UNSPSC) es obligatorio", null));
            }
            i++;
        }
        return r.isEmpty() ? List.of(ResultadoRegla.ok(codigo())) : r;
    }

    private ResultadoRegla err(int linea, String campo, String mensaje, BigDecimal valor) {
        return ResultadoRegla.error(codigo(), Severidad.ERROR, mensaje, linea, campo, null,
                valor == null ? null : valor.toPlainString());
    }
}
