package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record ExogenaRenglonDto(
        Integer ano,
        String formato,
        Long terceroId,
        String tipoDocumento,
        String numeroDocumento,
        String digitoVerificacion,
        String razonSocial,
        String municipio,
        String pais,
        Long conceptoId,
        String codigoConcepto,
        String descripcionConcepto,
        BigDecimal valor,
        Integer cantidadMovimientos) {
}
