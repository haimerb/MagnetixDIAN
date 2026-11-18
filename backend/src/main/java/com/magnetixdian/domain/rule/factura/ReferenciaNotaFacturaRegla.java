package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla FE-007: las notas crédito (91) y débito (92) deben referenciar el
 * documento que modifican (número y CUFE de la factura original).
 */
@Component
public class ReferenciaNotaFacturaRegla implements ReglaFactura {

    @Override
    public String codigo() {
        return "FE-007";
    }

    @Override
    public String nombre() {
        return "Documento de referencia en notas crédito/débito";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoFactura ctx) {
        var f = ctx.factura();
        if (!f.tipo().esNota()) {
            return List.of(ResultadoRegla.ok(codigo()));
        }
        List<ResultadoRegla> r = new ArrayList<>();
        if (f.numeroReferencia() == null || f.numeroReferencia().isBlank()) {
            r.add(error("numeroReferencia"));
        }
        if (f.cufeReferencia() == null || f.cufeReferencia().isBlank()) {
            r.add(error("cufeReferencia"));
        }
        return r.isEmpty() ? List.of(ResultadoRegla.ok(codigo())) : r;
    }

    private ResultadoRegla error(String campo) {
        return ResultadoRegla.error(codigo(), Severidad.ERROR,
                "La nota debe referenciar el documento original: campo '" + campo + "' obligatorio",
                null, campo, "valor requerido", null);
    }
}
