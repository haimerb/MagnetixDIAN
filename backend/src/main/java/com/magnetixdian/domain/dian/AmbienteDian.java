package com.magnetixdian.domain.dian;

public enum AmbienteDian {
    PRODUCCION(1),
    PRUEBAS(2);

    private final int codigo;

    AmbienteDian(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }

    public static AmbienteDian desde(int codigo) {
        return switch (codigo) {
            case 1 -> PRODUCCION;
            case 2 -> PRUEBAS;
            default -> throw new IllegalArgumentException("Ambiente DIAN no soportado: " + codigo);
        };
    }
}
