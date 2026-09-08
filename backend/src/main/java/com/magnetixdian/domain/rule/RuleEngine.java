package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Motor de reglas: ejecuta todas las {@link ReglaValidacion} descubiertas
 * como beans de Spring sobre un contexto. Las reglas se indexan por código.
 *
 * <p>Extensible por diseño: basta registrar un nuevo bean {@code ReglaValidacion}
 * (con codigo existente en la tabla {@code regla}) para que se ejecute junto a
 * las demás, sin modificar este motor.</p>
 */
@Component
public class RuleEngine {

    private final Map<String, ReglaValidacion> reglas;

    public RuleEngine(Collection<ReglaValidacion> reglas) {
        this.reglas = reglas.stream()
                .collect(Collectors.toMap(ReglaValidacion::codigo, Function.identity(), (a, b) -> b));
    }

    public List<ReglaValidacion> reglasActivas() {
        return reglas.values().stream()
                .sorted(Comparator.comparing(ReglaValidacion::codigo))
                .toList();
    }

    /**
     * Ejecuta todas las reglas sobre un contexto de validación.
     */
    public List<ResultadoRegla> validar(ContextoValidacion contexto) {
        List<ResultadoRegla> resultados = new ArrayList<>();
        for (ReglaValidacion regla : reglas.values()) {
            resultados.addAll(regla.evaluar(contexto));
        }
        return resultados;
    }
}