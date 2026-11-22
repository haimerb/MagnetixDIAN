package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ConciliacionDto(
        Long id,
        Long empresaId,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        int totalDocumentos,
        int coincidencias,
        int diferencias,
        int documentosSinAsiento,
        BigDecimal valorDiferencia,
        OffsetDateTime creadoEn,
        List<ConciliacionLineaDto> lineas
) {
}
