package com.magnetixdian.interfaces.dto;

import java.util.List;

public record SolicitudCatalogoExogenaDto(
        Integer ano,
        String formato,
        List<DianConceptoDto> conceptos,
        List<DianCuentaConceptoDto> cuentas) {
}
