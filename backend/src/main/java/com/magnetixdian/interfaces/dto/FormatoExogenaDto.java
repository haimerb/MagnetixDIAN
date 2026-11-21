package com.magnetixdian.interfaces.dto;

import java.util.List;

public record FormatoExogenaDto(
        String codigo,
        String descripcion,
        String estado,
        boolean activo,
        String version,
        String xsd,
        String validacion,
        String generadorXml,
        List<String> encabezados
) {
}
