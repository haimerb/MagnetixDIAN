package com.magnetixdian.application.pago;

import java.math.BigDecimal;

public record CheckoutResponseDto(
        String checkoutUrl,
        String referencia,
        BigDecimal monto,
        String moneda,
        String estado,
        String metodo,
        String tipo,
        String qrImage,
        String qrId,
        String transaccionId) {

    public CheckoutResponseDto(String checkoutUrl, String referencia, BigDecimal monto, String moneda, String estado) {
        this(checkoutUrl, referencia, monto, moneda, estado, null, null, null, null, null);
    }
}
