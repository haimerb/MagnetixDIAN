package com.magnetixdian.interfaces.dto;

public record CuentaContableDto(
        Long id,
        String codigo,
        String nombre,
        String tipo,
        String naturaleza,
        Integer nivel,
        String versionPuc,
        boolean aceptaAuxiliar,
        boolean activa,
        Long cuentaPadreId) {
}
