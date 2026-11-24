package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
public record CuentaBancariaRequestDto(String banco, String numeroCuenta, String tipoCuenta, String moneda, BigDecimal saldoInicial, Boolean activa) {}
