package com.magnetixdian.interfaces.dto;

import java.util.List;

public record ResultadoExogenaGenerarDto(
        Long generacionId,
        String estado,
        Integer ano,
        String formato,
        Integer totalRenglones,
        Integer totalValidaciones,
        boolean prevalidacionOficial,
        String xml,
        byte[] excel,
        String mensaje,
        List<ExogenaValidacionDto> validaciones) {
}
