package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;

public record EventoAuditoriaDto(
        Long id,
        String usuario,
        String accion,
        String entidad,
        String entidadId,
        String detalle,
        String ip,
        OffsetDateTime createdAt
) {
}
