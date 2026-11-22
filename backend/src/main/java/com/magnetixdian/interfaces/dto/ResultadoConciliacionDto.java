package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResultadoConciliacionDto(
        Long conciliacionId,
        Long empresaId,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        int totalDocumentos,
        int coincidencias,
        int diferencias,
        int documentosSinAsiento,
        BigDecimal valorDiferencia,
        List<ConciliacionLineaDto> lineas) {
}
