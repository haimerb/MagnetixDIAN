package com.magnetixdian.interfaces.dto;

public record ResultadoCatalogoExogenaDto(
        Integer ano,
        String formato,
        Integer conceptosGuardados,
        Integer relacionesGuardadas) {
}
