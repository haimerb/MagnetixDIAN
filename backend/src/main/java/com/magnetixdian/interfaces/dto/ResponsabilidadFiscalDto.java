package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

public record ResponsabilidadFiscalDto(
        Long id,
        Long empresaId,
        Long terceroId,
        String impuestoCodigo,
        String codigo,
        LocalDate desde,
        LocalDate hasta) {
}
