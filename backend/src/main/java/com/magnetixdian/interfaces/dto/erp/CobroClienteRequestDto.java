package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CobroClienteRequestDto(Long pedidoId, Long cuentaBancariaId, String clienteId, String clienteNombre, LocalDate fecha, BigDecimal valor, String medioPago, String referencia, String observaciones) {}
