package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record ActivoDto(Long id, Long empresaId, String codigo, String nombre, String categoria, LocalDate fechaAlta, BigDecimal valorAdquisicion, BigDecimal valorResidual, Integer vidaUtilMeses, BigDecimal depreciacionAcumulada, BigDecimal valorLibros, String estado, LocalDate fechaBaja, String observaciones, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
