package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Regla FE-005: el número del documento debe estar dentro del rango autorizado
 * y la fecha de emisión dentro de la vigencia de la resolución.
 */
@Component
public class RangoNumeracionFacturaRegla implements ReglaFactura {

    @Override
    public String codigo() {
        return "FE-005";
    }

    @Override
    public String nombre() {
        return "Rango de numeración y vigencia de la resolución";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoFactura ctx) {
        var f = ctx.factura();
        List<ResultadoRegla> r = new ArrayList<>();

        if (f.rangoDesde() > 0 && f.numero() < f.rangoDesde()) {
            r.add(err("numero", String.valueOf(f.rangoDesde()), String.valueOf(f.numero()),
                    "El número está por debajo del rango autorizado"));
        }
        if (f.rangoHasta() > 0 && f.numero() > f.rangoHasta()) {
            r.add(err("numero", "<= " + f.rangoHasta(), String.valueOf(f.numero()),
                    "El número supera el rango autorizado"));
        }
        if (f.rangoVigenciaDesde() != null && f.fechaEmision() != null
                && f.fechaEmision().isBefore(f.rangoVigenciaDesde())) {
            r.add(err("fechaEmision", f.rangoVigenciaDesde().toString(),
                    f.fechaEmision().toString(), "La fecha es anterior a la vigencia de la resolución"));
        }
        if (f.rangoVigenciaHasta() != null && f.fechaEmision() != null
                && f.fechaEmision().isAfter(f.rangoVigenciaHasta())) {
            r.add(err("fechaEmision", f.rangoVigenciaHasta().toString(),
                    f.fechaEmision().toString(), "La fecha supera la vigencia de la resolución"));
        }
        if (f.resolucionNumero() == null || f.resolucionNumero().isBlank()) {
            r.add(ResultadoRegla.error(codigo(), Severidad.ADVERTENCIA,
                    "El documento no referencia una resolución de facturación",
                    null, "resolucionNumero", null, null));
        }
        return r.isEmpty() ? List.of(ResultadoRegla.ok(codigo())) : r;
    }

    private ResultadoRegla err(String campo, String esperado, String encontrado, String mensaje) {
        return ResultadoRegla.error(codigo(), Severidad.ERROR, mensaje, null, campo, esperado, encontrado);
    }
}
