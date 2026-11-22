package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MigracionContableDto(
        Long id,
        String proveedor,
        List<String> etapas,
        String estado,
        int exitosos,
        int omitidos,
        int errores,
        int total,
        String mensaje,
        OffsetDateTime createdAt) {
}
