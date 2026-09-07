package com.magnetixdian.domain.model;

/**
 * Resultado de la evaluación de una regla sobre un registro concreto.
 * Si {@code valida} es {@code false}, {@code mensaje} describe el problema
 * y {@code severidad} indica su impacto.
 */
public record ResultadoRegla(
        String codigoRegla,
        boolean valida,
        Severidad severidad,
        String mensaje,
        Integer linea,
        String campo,
        String valorEsperado,
        String valorEncontrado
) {
    public static ResultadoRegla ok(String codigoRegla) {
        return new ResultadoRegla(codigoRegla, true, null, null, null, null, null, null);
    }

    public static ResultadoRegla error(String codigoRegla, Severidad severidad, String mensaje,
                                       Integer linea, String campo,
                                       String valorEsperado, String valorEncontrado) {
        return new ResultadoRegla(codigoRegla, false, severidad, mensaje, linea, campo, valorEsperado, valorEncontrado);
    }
}