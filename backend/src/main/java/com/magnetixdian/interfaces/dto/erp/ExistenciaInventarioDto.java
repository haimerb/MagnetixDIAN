package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record ExistenciaInventarioDto(Long empresaId, String bodega, String productoId, BigDecimal existencia, BigDecimal costoPromedio, OffsetDateTime updatedAt) {}
