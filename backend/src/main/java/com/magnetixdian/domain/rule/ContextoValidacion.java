package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.OperacionDatos;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Contexto de ejecución de una regla: contiene el registro a validar, el
 * formato del medio y parámetros por vigencia (UVT, catálogo de conceptos,
 * cuantías menores).
 */
public record ContextoValidacion(
        OperacionDatos operacion,
        BigDecimal valorUvt,
        Map<String, String> conceptosValidos,
        BigDecimal cuantiaMenor,
        String formato
) {

    public static final String FORMATO_DEFECTO = "1001";

    /**
     * Constructor compatible con el formato 1001 (formato por defecto).
     */
    public ContextoValidacion(OperacionDatos operacion, BigDecimal valorUvt,
                              Map<String, String> conceptosValidos, BigDecimal cuantiaMenor) {
        this(operacion, valorUvt, conceptosValidos, cuantiaMenor, FORMATO_DEFECTO);
    }

    public String formato() {
        return formato == null || formato.isBlank() ? FORMATO_DEFECTO : formato;
    }

    /**
     * Convierte un monto en UVT a pesos usando la UVT vigente del año gravable.
     */
    public BigDecimal uvtAPesos(BigDecimal uvt) {
        return uvt.multiply(valorUvt);
    }

    /**
     * Indica si el concepto pertenece al catálogo vigente.
     */
    public boolean conceptoExiste(String concepto) {
        return concepto != null && conceptosValidos.containsKey(concepto.trim());
    }
}