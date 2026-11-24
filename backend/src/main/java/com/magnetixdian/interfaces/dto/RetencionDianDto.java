package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record RetencionDianDto(
        String codigo,
        String nombre,
        BigDecimal base,
        BigDecimal porcentaje,
        BigDecimal valor) {
}
