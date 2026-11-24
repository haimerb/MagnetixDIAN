package com.magnetixdian.interfaces.dto;

public record SolicitudTerceroDto(
        String tipoDocumento,
        String numeroDocumento,
        String digitoVerificacion,
        String nombre,
        String municipio,
        String pais,
        Boolean activo) {
}
