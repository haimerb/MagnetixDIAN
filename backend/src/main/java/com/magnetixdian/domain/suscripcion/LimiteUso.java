package com.magnetixdian.domain.suscripcion;

/**
 * Reglas de límite de uso de un plan. Un límite {@code 0} significa ilimitado.
 */
public final class LimiteUso {

    private LimiteUso() {
    }

    /** Indica si el uso alcanzó el límite del plan. */
    public static boolean excedido(int usado, int limite) {
        return limite > 0 && usado >= limite;
    }

    /** Documentos/usuarios restantes; {@code null} cuando el plan es ilimitado. */
    public static Integer restante(int usado, int limite) {
        if (limite <= 0) {
            return null;
        }
        return Math.max(0, limite - usado);
    }
}
