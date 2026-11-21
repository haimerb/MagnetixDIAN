package com.magnetixdian.domain.exogena;

import java.util.List;

public record FormatoExogena(
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
