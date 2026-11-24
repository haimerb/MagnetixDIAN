package com.magnetixdian.interfaces.dto;

public record FormatoTributarioDto(
        String codigo,
        String descripcion,
        String estado,
        boolean activo,
        String xsd,
        boolean prevalidacionOficial) {
}
