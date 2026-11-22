package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DocumentoRadianDto(
        Long id,
        Long empresaId,
        String direccion,
        String tipoDocumento,
        String cufeCude,
        String consecutivo,
        String nitContraparte,
        String razonSocialContraparte,
        LocalDate fecha,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal retenciones,
        BigDecimal total,
        String estadoRadian,
        String estadoCausacion,
        String estadoContabilizacion,
        Long asientoContableId,
        OffsetDateTime createdAt,
        List<DocumentoRadianLineaDto> lineas,
        List<DocumentoRadianImpuestoDto> impuestosDetalle) {
}
