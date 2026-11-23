package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoImpuestoDto(
        Long id,
        Long causacionId,
        LocalDate fecha,
        BigDecimal valor,
        String comprobante) {
}
