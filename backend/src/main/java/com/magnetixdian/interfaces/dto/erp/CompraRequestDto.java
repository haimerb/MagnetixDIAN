package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CompraRequestDto(String numero, String proveedorId, String proveedorNombre, LocalDate fecha, BigDecimal subtotal, BigDecimal impuestos, BigDecimal total, String observaciones) {}
