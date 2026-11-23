package com.magnetixdian.interfaces.dto;

public record ResultadoGeneracionExogenaDto(
        String formato,
        String version,
        String descripcion,
        Integer registros,
        String xml,
        String validacion,
        boolean prevalidacionOficial
) {
}
