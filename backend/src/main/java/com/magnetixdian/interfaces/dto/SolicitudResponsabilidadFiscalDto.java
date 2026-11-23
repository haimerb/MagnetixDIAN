package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

public record SolicitudResponsabilidadFiscalDto(
        Long terceroId,
        String impuestoCodigo,
        String codigo,
        LocalDate desde,
        LocalDate hasta) {
}
