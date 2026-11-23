package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.util.List;

public record GrupoEstadoDto(String nombre, List<SaldoCuentaDto> cuentas, BigDecimal total) {
}
