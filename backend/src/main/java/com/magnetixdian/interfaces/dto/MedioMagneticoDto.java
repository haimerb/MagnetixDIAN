package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Representación de un medio magnético para listados (sin proxys JPA).
 */
public record MedioMagneticoDto(
        Long id,
        Long empresaId,
        String empresaNit,
        String empresaRazonSocial,
        String formato,
        String versionFormato,
        Integer anioGravable,
        String estado,
        LocalDate fechaLimite,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}