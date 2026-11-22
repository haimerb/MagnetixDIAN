package com.magnetixdian.interfaces.dto;

import java.util.List;

public record ResultadoMigracionDto(
        int exitosos,
        int omitidos,
        int errores,
        int total,
        List<MigracionContableDetalleDto> items) {
}
