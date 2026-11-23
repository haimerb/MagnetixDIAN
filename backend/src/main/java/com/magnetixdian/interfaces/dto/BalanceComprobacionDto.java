package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.util.List;

public record BalanceComprobacionDto(List<SaldoCuentaDto> cuentas,
                                     BigDecimal totalSaldoInicial,
                                     BigDecimal totalDebito,
                                     BigDecimal totalCredito,
                                     BigDecimal totalSaldoFinal) {
}
