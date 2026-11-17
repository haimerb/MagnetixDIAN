package com.magnetixdian.domain.factura;

/**
 * Tipos de documento electrónico DIAN soportados.
 * El código corresponde a cbc:InvoiceTypeCode / cbc:CreditNoteTypeCode.
 */
public enum TipoDocumento {

    FACTURA_VENTA("01", "Invoice", "DIAN 2.1: Factura Electrónica de Venta"),
    NOTA_CREDITO("91", "CreditNote", "DIAN 2.1: Nota Crédito de Factura Electrónica de Venta"),
    NOTA_DEBITO("92", "DebitNote", "DIAN 2.1: Nota Débito de Factura Electrónica de Venta"),
    DOCUMENTO_SOPORTE("05", "Invoice", "DIAN 2.1: Documento Soporte en adquisiciones a no obligados");

    private final String codigo;
    private final String ublRoot;
    private final String profileId;

    TipoDocumento(String codigo, String ublRoot, String profileId) {
        this.codigo = codigo;
        this.ublRoot = ublRoot;
        this.profileId = profileId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getUblRoot() {
        return ublRoot;
    }

    public String getProfileId() {
        return profileId;
    }

    public boolean esNota() {
        return this == NOTA_CREDITO || this == NOTA_DEBITO;
    }

    public static TipoDocumento porCodigo(String codigo) {
        for (TipoDocumento t : values()) {
            if (t.codigo.equals(codigo)) {
                return t;
            }
        }
        return FACTURA_VENTA;
    }
}
