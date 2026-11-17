package com.magnetixdian.domain.factura;

import java.math.BigDecimal;

/**
 * Impuesto o retención aplicado al documento (desglose por tributo).
 */
public record ImpuestoFactura(
        String tipo,
        String nombre,
        BigDecimal base,
        BigDecimal porcentaje,
        BigDecimal valor) {
}
