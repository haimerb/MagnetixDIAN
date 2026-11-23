package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SolicitudCausacionImpuestoDto(
        String impuestoCodigo,
        String origen,
        Long origenId,
        Long terceroId,
        LocalDate fecha,
        String conceptoCodigo,
        BigDecimal baseImponible,
        String observaciones) {
}
