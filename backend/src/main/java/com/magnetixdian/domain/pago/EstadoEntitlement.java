package com.magnetixdian.domain.pago;

import java.util.Locale;

public enum EstadoEntitlement {
    ACTIVA,
    SUSPENDIDA,
    CANCELADA;

    public static EstadoEntitlement desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el estado de la suscripción.");
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de suscripción no soportado: " + valor);
        }
    }
}
