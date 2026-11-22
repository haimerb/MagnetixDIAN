package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record ConciliacionLineaDto(
        Long documentoId,
        String consecutivo,
        Long asientoId,
        String resultado,
        String severidad,
        String campo,
        BigDecimal valorDocumento,
        BigDecimal valorAsiento,
        BigDecimal diferencia,
        String mensaje) {
}
