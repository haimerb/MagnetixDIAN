package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Resumen de un documento electrónico para listados.
 */
public record FacturaResumenDto(
        Long id,
        String tipoDocumento,
        String numeroCompleto,
        String cufe,
        String estado,
        LocalDate fechaEmision,
        String adquirenteNombre,
        String adquirenteNumDoc,
        BigDecimal total,
        BigDecimal totalPagar,
        OffsetDateTime createdAt) {
}
