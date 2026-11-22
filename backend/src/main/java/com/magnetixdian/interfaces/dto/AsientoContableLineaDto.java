package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record AsientoContableLineaDto(
        Long id,
        String codigoCuenta,
        String nombreCuenta,
        String descripcion,
        BigDecimal debito,
        BigDecimal credito) {
}
