package com.magnetixdian.interfaces.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Solicitud de emisión de un documento electrónico.
 */
public record FacturaRequest(
        String tipoDocumento,
        AdquirenteRequest adquirente,
        List<LineaFacturaRequest> lineas,
        String observaciones,
        String formaPago,
        String medioPago,
        String tipoOperacion,
        LocalDate fechaVencimiento,
        String cufeReferencia,
        String numeroReferencia,
        Long pedidoId) {
}
