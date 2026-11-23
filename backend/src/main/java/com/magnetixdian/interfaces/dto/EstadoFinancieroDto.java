package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EstadoFinancieroDto(String titulo,
                                  LocalDate fechaDesde,
                                  LocalDate fechaHasta,
                                  List<GrupoEstadoDto> grupos,
                                  BigDecimal total) {
}
