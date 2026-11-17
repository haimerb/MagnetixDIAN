package com.magnetixdian.domain.transmision;

/**
 * Resultado de la transmisión de un documento a la DIAN o al PAC.
 */
public record ResultadoTransmision(
        boolean exitoso,
        String estado,
        String cufe,
        String mensaje,
        String respuestaCruda) {

    public static ResultadoTransmision ok(String cufe, String mensaje, String crudo) {
        return new ResultadoTransmision(true, "ACEPTADO", cufe, mensaje, crudo);
    }

    public static ResultadoTransmision error(String mensaje, String crudo) {
        return new ResultadoTransmision(false, "RECHAZADO", null, mensaje, crudo);
    }
}
