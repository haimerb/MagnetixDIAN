package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;

public record CalendarioTributarioDto(
        Long id,
        String impuestoCodigo,
        Integer anioGravable,
        String periodicidad,
        String periodo,
        LocalDate fechaLimite) {
}
