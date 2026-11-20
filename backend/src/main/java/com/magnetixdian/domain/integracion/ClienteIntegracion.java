package com.magnetixdian.domain.integracion;

public record ClienteIntegracion(
        String idExterno,
        String nit,
        String razonSocial,
        String email,
        String telefono,
        String direccion) {
}
