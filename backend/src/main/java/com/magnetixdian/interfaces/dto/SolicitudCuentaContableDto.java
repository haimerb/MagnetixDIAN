package com.magnetixdian.interfaces.dto;

public record SolicitudCuentaContableDto(
        String codigo,
        String nombre,
        String tipo,
        String naturaleza,
        Integer nivel,
        String versionPuc,
        Boolean aceptaAuxiliar,
        Long cuentaPadreId,
        Boolean activa) {
}
