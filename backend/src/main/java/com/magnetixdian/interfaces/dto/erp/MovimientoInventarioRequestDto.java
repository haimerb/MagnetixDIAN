package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
public record MovimientoInventarioRequestDto(String tipo, String bodega, String productoId, String descripcion, BigDecimal cantidad, BigDecimal costoUnitario, LocalDate fecha, String referenciaTipo, String referenciaId, String observaciones) {}
