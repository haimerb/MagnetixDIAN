package com.magnetixdian.domain.radian;

public enum TipoDocumentoRadian {
    FACTURA("01"),
    NOTA_CREDITO("91"),
    NOTA_DEBITO("92"),
    DOCUMENTO_SOPORTE("05"),
    NOMINA("31");

    private final String codigo;

    TipoDocumentoRadian(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static TipoDocumentoRadian desde(String valor) {
        if (valor != null) {
            for (TipoDocumentoRadian tipo : values()) {
                if (tipo.codigo.equals(valor.trim())) {
                    return tipo;
                }
            }
        }
        throw new IllegalArgumentException("Tipo de documento RADIAN inválido: " + valor);
    }
}
