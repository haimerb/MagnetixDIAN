package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record PlanDto(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precioMensual,
        String moneda,
        int limiteFacturasMes,
        int limiteUsuarios,
        int orden) {
}
