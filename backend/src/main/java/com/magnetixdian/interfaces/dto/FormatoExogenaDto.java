package com.magnetixdian.interfaces.dto;

import java.util.List;

public record FormatoExogenaDto(
        String codigo,
        String descripcion,
        String estado,
        boolean activo,
        List<String> encabezados
) {
}
