package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CertificadoImpuestoDto(
        Long id,
        Long empresaId,
        Long terceroId,
        String impuestoCodigo,
        Integer anio,
        String numero,
        BigDecimal valor,
        LocalDate fechaEmision) {
}
