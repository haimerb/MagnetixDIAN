package com.magnetixdian.domain.integracion;

import java.math.BigDecimal;

public record LineaFacturaIntegracion(
        String productoId,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal valorUnitario,
        BigDecimal impuesto) {
}
