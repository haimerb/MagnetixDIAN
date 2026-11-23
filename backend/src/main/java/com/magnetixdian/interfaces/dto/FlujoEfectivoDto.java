package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FlujoEfectivoDto(LocalDate fechaDesde,
                               LocalDate fechaHasta,
                               List<MovimientoContableDto> movimientos,
                               BigDecimal entradas,
                               BigDecimal salidas,
                               BigDecimal neto) {
}
