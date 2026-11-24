package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record ResumenContableDto(
        String codigo,
        String nombre,
        BigDecimal debito,
        BigDecimal credito,
        BigDecimal saldo,
        Integer cantidadAsientos) {
}
