package com.magnetixdian.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NominaElectronicaDto(
        String trabajadorTipoDocumento,
        String trabajadorNumeroDocumento,
        String trabajadorNombre,
        String periodo,
        LocalDate fechaPago,
        List<ConceptoDto> devengados,
        List<ConceptoDto> deducciones) {

    public record ConceptoDto(String codigo, String descripcion, BigDecimal valor) {
    }
}
