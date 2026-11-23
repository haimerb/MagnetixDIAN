package com.magnetixdian.interfaces.dto;

public record ResultadoValidacionExogenaDto(
        String formato,
        boolean valido,
        String mensaje,
        boolean prevalidacionOficial
) {
}
