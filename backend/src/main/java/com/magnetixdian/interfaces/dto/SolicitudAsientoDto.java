package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;
import java.util.List;

public record SolicitudAsientoDto(
        LocalDate fecha,
        String descripcion,
        String tipoEvento,
        List<SolicitudLineaAsientoDto> lineas,
        String origen,
        Long origenId) {
}
