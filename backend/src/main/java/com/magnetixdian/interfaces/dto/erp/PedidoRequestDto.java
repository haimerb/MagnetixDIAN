package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PedidoRequestDto(String numero, Long ordenVentaId, String clienteId, String clienteNombre, LocalDate fecha, BigDecimal subtotal, BigDecimal impuestos, BigDecimal total, String observaciones) {}
