package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record DianConceptoDto(
        Long id,
        Integer ano,
        String formato,
        String codigo,
        String descripcion,
        String tipoTercero,
        String tipoDocumento,
        BigDecimal valorMinimo,
        boolean activo) {
}
