package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record ResultadoCalculoImpuestoDto(
        String impuestoCodigo,
        String nombreImpuesto,
        Integer anio,
        String conceptoCodigo,
        BigDecimal base,
        BigDecimal uvt,
        BigDecimal baseMinima,
        BigDecimal baseMaxima,
        BigDecimal baseGravable,
        BigDecimal tarifa,
        boolean aplica,
        BigDecimal valor) {
}
