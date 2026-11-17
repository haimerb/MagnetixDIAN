package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de validación de documentos electrónicos. Ejecuta todas las
 * {@link ReglaFactura} descubiertas como beans de Spring.
 */
@Component
public class ValidadorFactura {

    private final List<ReglaFactura> reglas;

    public ValidadorFactura(List<ReglaFactura> reglas) {
        this.reglas = reglas;
    }

    public List<ResultadoRegla> validar(ContextoFactura contexto) {
        List<ResultadoRegla> resultados = new ArrayList<>();
        for (ReglaFactura regla : reglas) {
            resultados.addAll(regla.evaluar(contexto));
        }
        return resultados;
    }

    public boolean tieneErrores(List<ResultadoRegla> resultados) {
        return resultados.stream().anyMatch(x -> !x.valida() && x.severidad() == Severidad.ERROR);
    }
}
