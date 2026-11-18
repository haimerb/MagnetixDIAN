package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;

/**
 * Evento DIAN registrado sobre un documento electrónico.
 */
public record FacturaEventoDto(
        Long id,
        String tipoEvento,
        String descripcion,
        String estado,
        OffsetDateTime fecha) {
}
