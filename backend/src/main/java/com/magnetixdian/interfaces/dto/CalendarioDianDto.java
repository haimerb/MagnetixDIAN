package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

/**
 * Fecha de vencimiento del calendario DIAN.
 */
public record CalendarioDianDto(
        Long id,
        Integer anioGravable,
        Integer anioPresentacion,
        String tipoReporte,
        String rangoNitIni,
        String rangoNitFin,
        LocalDate fechaLimite
) {
}