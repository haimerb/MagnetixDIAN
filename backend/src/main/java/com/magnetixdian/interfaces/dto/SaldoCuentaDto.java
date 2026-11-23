package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;

public record SaldoCuentaDto(String codigo,
                             String nombre,
                             String tipo,
                             String naturaleza,
                             BigDecimal saldoInicial,
                             BigDecimal debito,
                             BigDecimal credito,
                             BigDecimal saldoFinal) {
}
