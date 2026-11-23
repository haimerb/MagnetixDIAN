package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record TarifaImpuestoDto(
        Long id,
        String impuestoCodigo,
        String nombre,
        Integer anio,
        String conceptoCodigo,
        BigDecimal porcentaje,
        BigDecimal baseMinUvt,
        BigDecimal baseMaxUvt,
        boolean activo) {
}
