package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record MovimientoInventarioDto(Long id, Long empresaId, String tipo, String bodega, String productoId, String descripcion, BigDecimal cantidad, BigDecimal costoUnitario, BigDecimal costoPromedio, BigDecimal existenciaAnterior, BigDecimal existenciaNueva, LocalDate fecha, String referenciaTipo, String referenciaId, String estado, String observaciones, OffsetDateTime createdAt) {}
