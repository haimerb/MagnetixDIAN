package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record CuentaBancariaDto(Long id, Long empresaId, String banco, String numeroCuenta, String tipoCuenta, String moneda, BigDecimal saldo, boolean activa, String estado, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
