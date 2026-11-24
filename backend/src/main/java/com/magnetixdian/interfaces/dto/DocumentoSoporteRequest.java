package com.magnetixdian.interfaces.dto;

import java.util.List;

public record DocumentoSoporteRequest(
        String proveedorTipoDocumento,
        String proveedorNumeroDocumento,
        String proveedorNombre,
        String proveedorEmail,
        String concepto,
        List<LineaFacturaRequest> lineas,
        List<RetencionDianDto> retenciones,
        String formaPago,
        String medioPago) {
}
