package com.magnetixdian.interfaces.dto.erp;
import java.time.LocalDate;
public record BajaActivoRequestDto(LocalDate fechaBaja, String motivo) {}
