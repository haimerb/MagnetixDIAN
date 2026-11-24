package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PagoProveedorRequestDto(Long compraId, Long cuentaBancariaId, String proveedorId, String proveedorNombre, LocalDate fecha, BigDecimal valor, String medioPago, String referencia, String observaciones) {}
