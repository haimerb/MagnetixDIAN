package com.magnetixdian.domain.pago;

import java.math.BigDecimal;

/**
 * Evento normalizado de una pasarela. {@code estado} usa APROBADO, RECHAZADO o
 * PENDIENTE.
 */
public record EventoPago(String referencia, String estado, BigDecimal monto) {

    public boolean aprobado() {
        return "APROBADO".equalsIgnoreCase(estado);
    }
}
