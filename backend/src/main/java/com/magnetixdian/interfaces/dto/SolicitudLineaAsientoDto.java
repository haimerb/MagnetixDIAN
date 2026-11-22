package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record SolicitudLineaAsientoDto(
        String codigoCuenta,
        String descripcion,
        BigDecimal debito,
        BigDecimal credito,
        Long centroCostoId,
        Long terceroId,
        BigDecimal baseImponible,
        String impuestoCodigo) {
}
