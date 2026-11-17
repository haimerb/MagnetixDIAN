package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla FE-002: coherencia del dígito de verificación del NIT del emisor y del
 * adquirente (cuando el tipo de documento es NIT, código 31).
 */
@Component
public class NitFacturaRegla implements ReglaFactura {

    private static final String TIPO_NIT = "31";

    @Override
    public String codigo() {
        return "FE-002";
    }

    @Override
    public String nombre() {
        return "Dígito de verificación del NIT";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoFactura ctx) {
        var f = ctx.factura();
        List<ResultadoRegla> r = new ArrayList<>();
        if (f.emisor() != null && TIPO_NIT.equals(f.emisor().tipoDocumento())) {
            r.addAll(validar("emisor", f.emisor().numeroIdentificacion(), f.emisor().digitoVerificacion()));
        }
        if (f.adquirente() != null && TIPO_NIT.equals(f.adquirente().tipoDocumento())) {
            r.addAll(validar("adquirente", f.adquirente().numeroDocumento(), f.adquirente().digitoVerificacion()));
        }
        return r.isEmpty() ? List.of(ResultadoRegla.ok(codigo())) : r;
    }

    private List<ResultadoRegla> validar(String campo, String nit, Integer dv) {
        if (nit == null || nit.isBlank()) {
            return List.of();
        }
        Integer esperado = NitDigitoVerificacionRegla.calcularDv(nit.replaceAll("[^0-9]", ""));
        if (esperado == null) {
            return List.of();
        }
        if (dv == null || !esperado.equals(dv)) {
            return List.of(ResultadoRegla.error(codigo(), Severidad.ERROR,
                    "Dígito de verificación incorrecto para el NIT del " + campo,
                    null, campo + ".digitoVerificacion",
                    String.valueOf(esperado), dv == null ? null : String.valueOf(dv)));
        }
        return List.of();
    }
}
