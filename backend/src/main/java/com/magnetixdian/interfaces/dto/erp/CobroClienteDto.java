package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record CobroClienteDto(Long id, Long empresaId, Long pedidoId, Long cuentaBancariaId, String clienteId, String clienteNombre, LocalDate fecha, BigDecimal valor, String medioPago, String referencia, String estado, String observaciones, OffsetDateTime createdAt) {}
