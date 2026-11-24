package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record DepreciacionDto(Long id, Long empresaId, Long activoId, String codigoActivo, String periodo, LocalDate fecha, BigDecimal valor, BigDecimal depreciacionAcumulada, BigDecimal valorLibros, String estado, OffsetDateTime createdAt) {}
