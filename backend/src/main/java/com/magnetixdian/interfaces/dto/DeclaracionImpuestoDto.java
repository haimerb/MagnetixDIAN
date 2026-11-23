package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DeclaracionImpuestoDto(
        Long id,
        Long empresaId,
        String impuestoCodigo,
        Integer anioGravable,
        String periodo,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        LocalDate fechaLimite,
        String estado,
        BigDecimal totalCausado,
        BigDecimal totalPagado,
        BigDecimal saldo) {
}
