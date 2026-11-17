package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

/**
 * Línea de detalle enviada al emitir un documento electrónico.
 */
public record LineaFacturaRequest(
        String codigoProducto,
        String descripcion,
        BigDecimal cantidad,
        String unidadMedida,
        BigDecimal valorUnitario,
        BigDecimal descuento,
        BigDecimal ivaPct,
        BigDecimal incPct,
        String codigoUnspsc) {
}
