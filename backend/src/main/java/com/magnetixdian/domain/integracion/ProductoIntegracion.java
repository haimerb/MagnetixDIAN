package com.magnetixdian.domain.integracion;

import java.math.BigDecimal;

public record ProductoIntegracion(
        String idExterno,
        String sku,
        String nombre,
        BigDecimal precio,
        String impuesto) {
}
