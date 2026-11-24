package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

public record PeriodoContableDto(
        Long id,
        Integer anio,
        Integer mes,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        String estado) {
}
