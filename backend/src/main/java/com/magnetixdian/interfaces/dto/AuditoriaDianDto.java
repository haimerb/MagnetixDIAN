package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;

public record AuditoriaDianDto(
        Long id,
        String accion,
        String detalle,
        String usuario,
        OffsetDateTime createdAt) {
}
