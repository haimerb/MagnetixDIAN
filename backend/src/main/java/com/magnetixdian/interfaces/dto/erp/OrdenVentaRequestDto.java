package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
public record OrdenVentaRequestDto(String numero, String clienteId, String clienteNombre, LocalDate fecha, BigDecimal subtotal, BigDecimal impuestos, BigDecimal total, String moneda, String observaciones) {}
