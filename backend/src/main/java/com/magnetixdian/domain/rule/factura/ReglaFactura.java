package com.magnetixdian.domain.rule.factura;

import com.magnetixdian.domain.model.ResultadoRegla;

import java.util.List;

/**
 * Estrategia de validación de un documento electrónico (FEV, nota crédito/débito
 * o documento soporte). Las reglas se descubren como beans de Spring y las
 * ejecuta {@link ValidadorFactura}.
 */
public interface ReglaFactura {

    String codigo();

    String nombre();

    List<ResultadoRegla> evaluar(ContextoFactura contexto);
}
