package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentoRadianRecepcionDto(
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
        String estadoRadian) {
}
