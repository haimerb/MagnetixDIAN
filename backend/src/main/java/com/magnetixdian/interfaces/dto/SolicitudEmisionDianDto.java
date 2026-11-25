package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;
import java.util.List;

public record SolicitudEmisionDianDto(
        String tipoDocumento,
        AdquirenteRequest adquirente,
        List<LineaFacturaRequest> lineas,
        List<RetencionDianDto> retenciones,
        String observaciones,
        String formaPago,
        String medioPago,
        String tipoOperacion,
        LocalDate fechaVencimiento,
        String cufeReferencia,
        String numeroReferencia,
        String motivo,
        NominaElectronicaDto nomina,
        Long pedidoId) {

    public static SolicitudEmisionDianDto desde(FacturaRequest req) {
        return new SolicitudEmisionDianDto(
                req.tipoDocumento(),
                req.adquirente(),
                req.lineas(),
                List.of(),
                req.observaciones(),
                req.formaPago(),
                req.medioPago(),
                req.tipoOperacion(),
                req.fechaVencimiento(),
                req.cufeReferencia(),
                req.numeroReferencia(),
                null,
                null,
                req.pedidoId());
    }
}
