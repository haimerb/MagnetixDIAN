package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record PagoDto(
        Long id,
        String referencia,
        String proveedor,
        String planCodigo,
        BigDecimal monto,
        String moneda,
        String estado,
        String url) {
}
