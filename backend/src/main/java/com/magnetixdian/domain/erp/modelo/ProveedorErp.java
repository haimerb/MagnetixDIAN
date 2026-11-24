package com.magnetixdian.domain.erp.modelo;

import java.util.Locale;
import java.util.Optional;

public enum ProveedorErp {
    SIIGO,
    WORLD_OFFICE,
    HELISA,
    ALEGRA;

    public static Optional<ProveedorErp> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return Optional.of(ProveedorErp.valueOf(normalizado));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
