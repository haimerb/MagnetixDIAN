package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

public record SolicitudPeriodoDto(
        Integer anio,
        Integer mes,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {
}
