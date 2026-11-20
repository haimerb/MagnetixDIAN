package com.magnetixdian.domain.pago;

public enum MetodoPago {
    CARD,
    NEQUI,
    PSE,
    BREB_QR,
    DAVIPLATA;

    public static MetodoPago desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return CARD;
        }
        try {
            return valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Método de pago no soportado: " + valor);
        }
    }
}
