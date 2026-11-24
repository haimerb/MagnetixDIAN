package com.magnetixdian.interfaces.dto;

import java.util.List;

public record SolicitudConciliacionTesoreriaDto(
        List<MovimientoTesoreriaDto> movimientosBanco) {
}
