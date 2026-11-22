package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record DocumentoRadianImpuestoDto(
        String tipo,
        String nombre,
        BigDecimal base,
        BigDecimal porcentaje,
        BigDecimal valor) {
}
