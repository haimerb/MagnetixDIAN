package com.magnetixdian.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Solicitud de alta de una fecha en el calendario DIAN.
 */
public record CalendarioDianRequest(
        @NotNull Integer anioGravable,
        @NotNull Integer anioPresentacion,
        @NotBlank @Size(max = 30) String tipoReporte,
        @Size(max = 2) String rangoNitIni,
        @Size(max = 2) String rangoNitFin,
        @NotNull LocalDate fechaLimite
) {
}