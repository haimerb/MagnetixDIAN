package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record ResumenImpuestoAsientoDto(
        String codigo,
        String nombre,
        BigDecimal base,
        BigDecimal valor,
        Integer cantidadAsientos) {
}
