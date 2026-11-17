package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Regla FE-001: campos obligatorios del documento (número, fecha, emisor,
 * adquirente y al menos una línea).
 */
@Component
public class ObligatoriedadFacturaRegla implements ReglaFactura {

    @Override
    public String codigo() {
        return "FE-001";
    }

    @Override
    public String nombre() {
        return "Campos obligatorios del documento electrónico";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoFactura ctx) {
        var f = ctx.factura();
        java.util.ArrayList<ResultadoRegla> r = new java.util.ArrayList<>();
        if (f.fechaEmision() == null) {
            r.add(error("fechaEmision"));
        }
        if (f.emisor() == null || f.emisor().numeroIdentificacion() == null
                || f.emisor().numeroIdentificacion().isBlank()) {
            r.add(error("emisor.numeroIdentificacion"));
        }
        if (f.adquirente() == null || f.adquirente().numeroDocumento() == null
                || f.adquirente().numeroDocumento().isBlank()) {
            r.add(error("adquirente.numeroDocumento"));
        }
        if (f.lineas() == null || f.lineas().isEmpty()) {
            r.add(error("lineas"));
        }
        return r.isEmpty() ? List.of(ResultadoRegla.ok(codigo())) : r;
    }

    private ResultadoRegla error(String campo) {
        return ResultadoRegla.error(codigo(), Severidad.ERROR,
                "El campo '" + campo + "' es obligatorio", null, campo, "valor requerido", null);
    }
}
