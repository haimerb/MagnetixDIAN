package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Detalle completo de un documento electrónico.
 */
public record FacturaDetalleDto(
        Long id,
        String tipoDocumento,
        String prefijo,
        long numero,
        String numeroCompleto,
        String cufe,
        String cude,
        int ambiente,
        String estado,
        LocalDate fechaEmision,
        LocalTime horaEmision,
        LocalDate fechaVencimiento,
        String moneda,
        String formaPago,
        String medioPago,
        AdquirenteRequest adquirente,
        List<LineaFacturaRequest> lineas,
        BigDecimal subtotal,
        BigDecimal totalImpuestos,
        BigDecimal totalRetenciones,
        BigDecimal total,
        BigDecimal totalPagar,
        String observaciones,
        String qrUrl,
        String respuestaDian,
        OffsetDateTime fechaValidacion,
        OffsetDateTime createdAt,
        List<FacturaEventoDto> eventos) {
}
