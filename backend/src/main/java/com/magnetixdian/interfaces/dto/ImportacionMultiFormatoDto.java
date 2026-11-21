package com.magnetixdian.interfaces.dto;

import java.util.List;

public record ImportacionMultiFormatoDto(
        Integer anioGravable,
        List<FormatoDetectadoDto> formatos,
        List<String> hojasOmitidas,
        int formatosPersistidos
) {
}
