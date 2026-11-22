package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;

public record MigracionContableDetalleDto(
        Long id,
        String etapa,
        String externalId,
        String estado,
        String error,
        String payload,
        OffsetDateTime createdAt) {
}
