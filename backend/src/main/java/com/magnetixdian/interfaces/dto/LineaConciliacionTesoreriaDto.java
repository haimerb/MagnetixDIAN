package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LineaConciliacionTesoreriaDto(
        LocalDate fecha,
        String referencia,
        String cuentaCodigo,
        String descripcion,
        BigDecimal valorLibro,
        BigDecimal valorBanco,
        String estado) {
}
