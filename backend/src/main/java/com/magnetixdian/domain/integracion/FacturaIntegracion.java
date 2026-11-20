package com.magnetixdian.domain.integracion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record FacturaIntegracion(
        String idExterno,
        String numero,
        LocalDate fecha,
        String clienteId,
        String clienteNit,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal total,
        String moneda,
        List<LineaFacturaIntegracion> lineas,
        Map<String, String> metadatos) {
}
