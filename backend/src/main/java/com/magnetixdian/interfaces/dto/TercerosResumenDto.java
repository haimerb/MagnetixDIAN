package com.magnetixdian.interfaces.dto;

import java.util.List;

/**
 * Resumen de terceros reportados en un medio magnético y sugerencias de DV.
 */
public record TercerosResumenDto(
        long totalRegistros,
        long tercerosUnicos,
        long nitsConDvIncorrecto,
        List<SugerenciaDvDto> sugerenciasDv
) {
}