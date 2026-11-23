package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CausacionImpuestoDto(
        Long id,
        Long empresaId,
        String impuestoCodigo,
        String origen,
        Long origenId,
        Long terceroId,
        String periodo,
        LocalDate fecha,
        String conceptoCodigo,
        BigDecimal baseImponible,
        BigDecimal tarifa,
        BigDecimal valor,
        String estado,
        BigDecimal valorPagado,
        String observaciones) {
}
