package com.magnetixdian.domain.integracion;

import java.math.BigDecimal;

public record PagoIntegracion(
        String referencia,
        BigDecimal monto,
        String moneda,
        String estado,
        String metodo) {
}
