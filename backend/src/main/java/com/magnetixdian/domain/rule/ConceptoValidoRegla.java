package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Regla: CONCEPTO_VALIDO — el concepto debe pertenecer al catálogo vigente
 * del formato 1001 para el año gravable.
 */
@Component
public class ConceptoValidoRegla implements ReglaValidacion {

    @Override
    public String codigo() {
        return "CONCEPTO_VALIDO";
    }

    @Override
    public String nombre() {
        return "Concepto válido para el formato";
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoValidacion contexto) {
        var op = contexto.operacion();
        if (op.concepto() == null || op.concepto().isBlank()) {
            return List.of();
        }
        String concepto = op.concepto().trim();
        if (contexto.conceptoExiste(concepto)) {
            return List.of(ResultadoRegla.ok(codigo()));
        }
        return List.of(ResultadoRegla.error(
                codigo(), Severidad.ERROR,
                "Concepto " + concepto + " no existe en el catálogo del formato 1001.",
                op.linea(), "concepto", "Concepto del catálogo vigente", concepto));
    }
}