package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoCausacionDto(
        Long documentoId,
        Long causacionId,
        String estado,
        LocalDate fechaCausacion,
        BigDecimal total,
        String mensaje) {
}
