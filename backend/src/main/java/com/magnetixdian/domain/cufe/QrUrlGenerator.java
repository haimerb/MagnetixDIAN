package com.magnetixdian.domain.cufe;

/**
 * Construye la URL del código QR de la representación gráfica (Anexo Técnico v1.9).
 * El parámetro {@code documentkey} contiene el CUFE/CUDE.
 */
public final class QrUrlGenerator {

    private static final String URL_PROD = "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=";
    private static final String URL_HAB = "https://catalogo-vpfe-hab.dian.gov.co/document/searchqr?documentkey=";

    private QrUrlGenerator() {
    }

    public static String generar(String cufe, int ambiente) {
        String base = ambiente == 1 ? URL_PROD : URL_HAB;
        return base + (cufe == null ? "" : cufe);
    }
}
