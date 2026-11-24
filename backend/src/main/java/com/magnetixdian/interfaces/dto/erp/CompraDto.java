package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record CompraDto(Long id, Long empresaId, String numero, String proveedorId, String proveedorNombre, LocalDate fecha, String estado, BigDecimal subtotal, BigDecimal impuestos, BigDecimal total, String observaciones, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
