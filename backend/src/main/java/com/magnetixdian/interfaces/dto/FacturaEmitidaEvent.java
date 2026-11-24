package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FacturaEmitidaEvent(
        Long empresaId,
        Long facturaId,
        String numeroCompleto,
        String tipoDocumento,
        String cufe,
        String cude,
        String trackId,
        String qrUrl,
        String estado,
        LocalDate fechaEmision,
        String terceroDocumento,
        Long terceroId,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal retenciones,
        BigDecimal total,
        BigDecimal totalPagar,
        String motivo) {
}
