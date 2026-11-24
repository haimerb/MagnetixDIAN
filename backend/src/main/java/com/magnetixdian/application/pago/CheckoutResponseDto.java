package com.magnetixdian.application.pago;

import java.math.BigDecimal;

public record CheckoutResponseDto(
        String checkoutUrl,
        String referencia,
        BigDecimal monto,
        String moneda,
        String estado) {
}
