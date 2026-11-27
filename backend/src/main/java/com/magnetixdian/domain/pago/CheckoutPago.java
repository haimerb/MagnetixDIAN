package com.magnetixdian.domain.pago;

public record CheckoutPago(
        String url,
        boolean confirmadoInmediato,
        String tipo,
        String qrImage,
        String qrId,
        String transaccionId) {

    public static final String TIPO_REDIRECCION = "REDIRECCION";
    public static final String TIPO_QR = "QR";
    public static final String TIPO_OTP = "OTP";

    public CheckoutPago(String url, boolean confirmadoInmediato) {
        this(url, confirmadoInmediato, TIPO_REDIRECCION, null, null, null);
    }

    public static CheckoutPago inmediato() {
        return new CheckoutPago(null, true);
    }

    public static CheckoutPago redireccion(String url) {
        return new CheckoutPago(url, false);
    }

    public static CheckoutPago qr(String qrId, String qrImage, String transaccionId) {
        return new CheckoutPago("wompi-qr:" + qrId, false, TIPO_QR, qrImage, qrId, transaccionId);
    }

    public static CheckoutPago otp(String url, String transaccionId) {
        return new CheckoutPago(url, false, TIPO_OTP, null, null, transaccionId);
    }
}
