package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClaveTercero1002Regla implements ReglaValidacion {

    @Override
    public String codigo() {
        return "CLAVE_TERCERO_1002";
    }

    @Override
    public String nombre() {
        return "Clave del tercero del registro 1002";
    }

    @Override
    public boolean aplicaA(String formato) {
        return "1002".equals(formato);
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
        var op = contexto.operacion();
        String tipoDocumento = op.tipoDocumento();
        if (tipoDocumento == null
                || (!"NIT".equalsIgnoreCase(tipoDocumento) && !"NITCE".equalsIgnoreCase(tipoDocumento))) {
            return List.of();
        }

        List<ResultadoRegla> resultados = new ArrayList<>();
        String numero = normalizar(op.numeroIdentificacion());
        if (numero.isEmpty() || !numero.chars().allMatch(Character::isDigit)) {
            resultados.add(ResultadoRegla.error(
                    codigo(),
                    Severidad.ERROR,
                    "La identificación del tercero 1002 debe ser numérica.",
                    op.linea(),
                    "numeroIdentificacion",
                    "Solo dígitos",
                    op.numeroIdentificacion() == null ? "vacío" : op.numeroIdentificacion()));
        }
        if (op.dv() == null) {
            resultados.add(ResultadoRegla.error(
                    codigo(),
                    Severidad.ERROR,
                    "La clave PerReinOtros del registro 1002 requiere el dígito de verificación.",
                    op.linea(),
                    "dv",
                    "DV presente",
                    "vacío"));
        }
        return resultados;
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace(".", "").replace("-", "").replace(" ", "").trim();
    }
}
