package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;

public record ApiKeyDto(
        Long id,
        String nombre,
        String prefijo,
        boolean activo,
        OffsetDateTime ultimoUso,
        OffsetDateTime createdAt) {
}
