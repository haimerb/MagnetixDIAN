package com.magnetixdian.interfaces.dto.erp;
import java.math.BigDecimal;
public record RecepcionLineaRequestDto(String productoId, String descripcion, BigDecimal cantidad, BigDecimal costoUnitario) {}
