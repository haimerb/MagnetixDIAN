package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AsientoContableDto(
        Long id,
        Long empresaId,
        String numero,
        LocalDate fecha,
        String descripcion,
        String documentoTipo,
        Long documentoId,
        String tipoEvento,
        String origen,
        Long origenId,
        String estado,
        BigDecimal totalDebito,
        BigDecimal totalCredito,
        BigDecimal valorTotal,
        BigDecimal valorImpuestos,
        BigDecimal valorRetenciones,
        OffsetDateTime createdAt,
        List<AsientoContableLineaDto> lineas) {
}
