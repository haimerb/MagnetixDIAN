package com.magnetixdian.domain.pago;

/**
 * Resultado de iniciar un checkout. {@code confirmadoInmediato} indica que el
 * pago se aprueba sin redirección (pasarela simulada).
 */
public record CheckoutPago(String url, boolean confirmadoInmediato) {

    public static CheckoutPago inmediato() {
        return new CheckoutPago(null, true);
    }

    public static CheckoutPago redireccion(String url) {
        return new CheckoutPago(url, false);
    }
}
