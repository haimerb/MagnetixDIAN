package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SolicitudPagoImpuestoDto(
        LocalDate fecha,
        BigDecimal valor,
        String comprobante) {
}
