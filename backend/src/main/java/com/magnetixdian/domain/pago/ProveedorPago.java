package com.magnetixdian.domain.pago;

import java.util.Locale;

public enum ProveedorPago {
    WOMPI,
    MERCADOPAGO,
    SIMULADO;

    public static ProveedorPago desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el proveedor de pago.");
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return valueOf(normalizado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Proveedor de pago no soportado: " + valor);
        }
    }
}
