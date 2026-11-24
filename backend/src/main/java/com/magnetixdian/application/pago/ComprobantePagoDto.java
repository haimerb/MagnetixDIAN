package com.magnetixdian.application.pago;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ComprobantePagoDto(
        String numero,
        LocalDate fecha,
        String nit,
        String plan,
        BigDecimal monto,
        BigDecimal impuestos,
        String estado,
        String urlPdf) {
}
