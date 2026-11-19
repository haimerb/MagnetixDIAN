package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;

public record EnvioFacturaDto(
        Long id,
        Long facturaId,
        String numeroCompleto,
        String cufe,
        String canal,
        String estado,
        int intentos,
        int maxIntentos,
        OffsetDateTime proximoIntento,
        String ultimoError,
        OffsetDateTime actualizadoAt
) {
}
