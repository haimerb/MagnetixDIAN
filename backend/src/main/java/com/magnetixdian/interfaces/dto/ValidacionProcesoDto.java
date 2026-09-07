package com.magnetixdian.interfaces.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resumen de una corrida de validación (respuesta de la API).
 */
public record ValidacionProcesoDto(
        Long id,
        Long medioMagneticoId,
        String estado,
        int erroresTotal,
        int advertenciasTotal,
        int registrosValidados,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        List<DetalleErrorDto> errores
) {

    public record DetalleErrorDto(
            Integer linea,
            String campo,
            String valorEsperado,
            String valorEncontrado,
            String mensaje,
            String severidad
    ) {
    }
}