package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record MovimientoTesoreriaDto(Long id, Long empresaId, Long cuentaBancariaId, String tipo, LocalDate fecha, BigDecimal valor, BigDecimal saldoAnterior, BigDecimal saldoNuevo, String referenciaTipo, String referenciaId, String estado, String descripcion, OffsetDateTime createdAt) {}
