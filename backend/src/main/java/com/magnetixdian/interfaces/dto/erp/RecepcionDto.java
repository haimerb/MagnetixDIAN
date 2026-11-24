package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
public record RecepcionDto(Long id, Long empresaId, Long compraId, String numero, LocalDate fecha, String bodega, String estado, BigDecimal total, String observaciones, OffsetDateTime createdAt, OffsetDateTime updatedAt, List<RecepcionLineaDto> lineas) {}
