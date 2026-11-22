package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DocumentoRadianImportDto(
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
        List<DocumentoRadianLineaDto> lineas,
        List<DocumentoRadianImpuestoDto> impuestosDetalle) {
}
