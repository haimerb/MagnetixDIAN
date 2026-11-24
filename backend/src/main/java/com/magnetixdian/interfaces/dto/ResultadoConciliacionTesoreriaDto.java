package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoConciliacionTesoreriaDto(
        BigDecimal saldoLibro,
        BigDecimal saldoBanco,
        BigDecimal diferencia,
        Integer coincidencias,
        List<LineaConciliacionTesoreriaDto> lineas) {
}
