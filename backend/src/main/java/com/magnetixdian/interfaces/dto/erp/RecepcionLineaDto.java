package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
public record RecepcionLineaDto(Long id, String productoId, String descripcion, BigDecimal cantidad, BigDecimal costoUnitario, BigDecimal total) {}
