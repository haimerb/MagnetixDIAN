package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record SolicitudCalculoImpuestoDto(
        String impuestoCodigo,
        Integer anio,
        String conceptoCodigo,
        BigDecimal base) {
}
