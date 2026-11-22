package com.magnetixdian.domain.dian;

import java.time.LocalDate;

public record SolicitudListadoDian(
        String token,
        byte[] archivo,
        String nombreArchivo,
        String direccion,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        String grupo,
        String nitTercero) {
}
