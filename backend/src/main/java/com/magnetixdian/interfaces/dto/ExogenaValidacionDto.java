package com.magnetixdian.interfaces.dto;

public record ExogenaValidacionDto(
        String severidad,
        String codigo,
        String mensaje,
        Long terceroId,
        Long conceptoId,
        String cuentaCodigo,
        String identificador) {
}
