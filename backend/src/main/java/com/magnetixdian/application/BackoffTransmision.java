package com.magnetixdian.application;

/**
 * Backoff exponencial (en minutos) para reintentos de transmisión.
 */
public final class BackoffTransmision {

    private static final int[] MINUTOS = {1, 5, 15, 60, 240};

    private BackoffTransmision() {
    }

    public static int minutos(int intento) {
        if (intento <= 0) {
            return MINUTOS[0];
        }
        return MINUTOS[Math.min(intento - 1, MINUTOS.length - 1)];
    }
}
