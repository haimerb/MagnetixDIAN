package com.magnetixdian.domain.rule;

import com.magnetixdian.domain.model.ResultadoRegla;

import java.util.List;

/**
 * Estrategia de validación de un registro de información exógena.
 * Cada implementación verifica una regla de negocio (DV del NIT, concepto,
 * topes UVT, consistencia de valores, campos obligatorios, etc.).
 *
 * <p>Las reglas se descubren como beans de Spring y se registran en el motor
 * por su {@link #codigo()} — codigo que debe existir en la tabla {@code regla}
 * para derivar severidad/mensajes configurables por vigencia.</p>
 */
public interface ReglaValidacion {

    /**
     * Código único de la regla. Debe coincidir con {@code regla.codigo} en BD.
     */
    String codigo();

    /**
     * Nombre legible de la regla.
     */
    String nombre();

    /**
     * Evalúa la regla sobre un registro de operación.
     *
     * @param contexto contexto con datos del registro y parámetros por vigencia
     * @return lista de resultados (normalmente uno; varios cuando una regla
     *         cubre múltiples campos de un mismo registro)
     */
    List<ResultadoRegla> evaluar(ContextoValidacion contexto);
}