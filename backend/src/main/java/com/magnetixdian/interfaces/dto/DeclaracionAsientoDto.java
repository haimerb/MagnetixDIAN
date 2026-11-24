package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DeclaracionAsientoDto(
        Integer anio,
        String periodo,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        BigDecimal totalBase,
        BigDecimal totalImpuesto,
        Integer totalAsientos,
        List<ResumenImpuestoAsientoDto> impuestos) {
}
