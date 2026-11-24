package com.magnetixdian.interfaces.dto;

public record TerceroContableDto(
        Long id,
        String tipoDocumento,
        String numeroDocumento,
        String digitoVerificacion,
        String nombre,
        String municipio,
        String pais,
        boolean activo) {
}
