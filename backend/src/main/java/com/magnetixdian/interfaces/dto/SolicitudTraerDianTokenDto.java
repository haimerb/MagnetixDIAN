package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

public record SolicitudTraerDianTokenDto(
        String token,
        String direccion,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        String grupo,
        String nitTercero,
        Boolean guardarToken) {
}
