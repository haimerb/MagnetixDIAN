package com.magnetixdian.domain.radian;

public enum DireccionDocumento {
    EMITIDO,
    RECIBIDO;

    public static DireccionDocumento desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return RECIBIDO;
        }
        for (DireccionDocumento direccion : values()) {
            if (direccion.name().equalsIgnoreCase(valor.trim())) {
                return direccion;
            }
        }
        throw new IllegalArgumentException("Dirección RADIAN inválida: " + valor);
    }
}
