package com.magnetixdian.interfaces.dto.erp;
import java.time.LocalDate;
import java.util.List;
public record RecepcionRequestDto(String numero, LocalDate fecha, String bodega, String observaciones, List<RecepcionLineaRequestDto> lineas) {}
