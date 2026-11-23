package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoContableDto(String codigoCuenta,
                                    String nombreCuenta,
                                    LocalDate fecha,
                                    String numeroAsiento,
                                    String descripcion,
                                    BigDecimal debito,
                                    BigDecimal credito,
                                    BigDecimal saldo,
                                    Long centroCostoId,
                                    Long terceroId) {
}
