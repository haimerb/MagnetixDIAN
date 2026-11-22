package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record DocumentoRadianLineaDto(
        Integer numeroLinea,
        String codigoProducto,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal valorUnitario,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal total) {
}
