package com.magnetixdian.domain.pago;

import java.math.BigDecimal;

public record SolicitudPago(
        Long empresaId,
        Long pagoId,
        String referencia,
        String planCodigo,
        BigDecimal monto,
        String moneda,
        String descripcion,
        String urlRetorno) {
}
