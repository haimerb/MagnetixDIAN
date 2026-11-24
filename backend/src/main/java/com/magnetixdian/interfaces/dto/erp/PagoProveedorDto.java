package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record PagoProveedorDto(Long id, Long empresaId, Long compraId, Long cuentaBancariaId, String proveedorId, String proveedorNombre, LocalDate fecha, BigDecimal valor, String medioPago, String referencia, String estado, String observaciones, OffsetDateTime createdAt) {}
