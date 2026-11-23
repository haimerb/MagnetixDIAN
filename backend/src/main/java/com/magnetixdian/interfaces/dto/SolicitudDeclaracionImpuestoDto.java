package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

public record SolicitudDeclaracionImpuestoDto(
        String impuestoCodigo,
        Integer anioGravable,
        String periodo,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {
}
