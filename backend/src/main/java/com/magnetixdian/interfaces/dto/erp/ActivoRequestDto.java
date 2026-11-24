package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ActivoRequestDto(String codigo, String nombre, String categoria, LocalDate fechaAlta, BigDecimal valorAdquisicion, BigDecimal valorResidual, Integer vidaUtilMeses, String observaciones) {}
