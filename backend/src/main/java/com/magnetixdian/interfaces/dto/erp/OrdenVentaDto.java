package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record OrdenVentaDto(Long id, Long empresaId, String numero, String clienteId, String clienteNombre, LocalDate fecha, String estado, BigDecimal subtotal, BigDecimal impuestos, BigDecimal total, String moneda, String observaciones, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
