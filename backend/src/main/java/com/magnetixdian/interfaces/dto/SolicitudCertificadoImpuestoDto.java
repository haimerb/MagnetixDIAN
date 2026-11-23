package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SolicitudCertificadoImpuestoDto(
        Long terceroId,
        String impuestoCodigo,
        Integer anio,
        String numero,
        BigDecimal valor,
        LocalDate fechaEmision) {
}
