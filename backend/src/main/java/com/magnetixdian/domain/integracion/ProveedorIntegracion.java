package com.magnetixdian.domain.integracion;

import java.util.Arrays;
import java.util.Locale;

public enum ProveedorIntegracion {
    SIIGO,
    ALEGRA,
    ODOO,
    WORLD_OFFICE,
    HELISA,
    SHOPIFY,
    WOOCOMMERCE,
    SAP_BUSINESS_ONE,
    DYNAMICS_365,
    NETSUITE;

    public String slug() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static java.util.Optional<ProveedorIntegracion> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return java.util.Optional.empty();
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return Arrays.stream(values())
                .filter(p -> p.name().equals(normalizado))
                .findFirst();
    }
}
