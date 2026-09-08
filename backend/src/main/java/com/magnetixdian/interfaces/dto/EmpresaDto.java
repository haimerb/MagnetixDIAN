package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;

/**
 * Representación de una empresa cliente (para listados y edición).
 */
public record EmpresaDto(
        Long id,
        String nit,
        String razonSocial,
        String regimen,
        String tipoDocumento,
        boolean granContribuyente,
        String direccion,
        String ciudad,
        String departamento,
        String codigoDane,
        String email,
        OffsetDateTime createdAt
) {
}