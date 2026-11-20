package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;

public record IntegracionEventoDto(
        Long id,
        String proveedor,
        String tipo,
        String estado,
        int intentos,
        OffsetDateTime proximoIntento,
        String ultimoError,
        OffsetDateTime creadoEn) {
}
