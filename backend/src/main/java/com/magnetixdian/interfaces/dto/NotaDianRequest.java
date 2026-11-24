package com.magnetixdian.interfaces.dto;

import java.util.List;

public record NotaDianRequest(
        AdquirenteRequest adquirente,
        List<LineaFacturaRequest> lineas,
        String numeroFactura,
        String cufeFactura,
        String motivo,
        String formaPago,
        String medioPago) {
}
