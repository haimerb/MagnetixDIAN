package com.magnetixdian.application;

import java.util.StringJoiner;

/**
 * Construye filas CSV compatibles con Excel en español (separador «;»).
 */
public final class CsvFacturas {

    private CsvFacturas() {
    }

    public static String fila(String... campos) {
        StringJoiner joiner = new StringJoiner(";");
        for (String campo : campos) {
            joiner.add(escapar(campo));
        }
        return joiner.toString();
    }

    static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        boolean requiereComillas = valor.indexOf(';') >= 0 || valor.indexOf('"') >= 0
                || valor.indexOf('\n') >= 0 || valor.indexOf('\r') >= 0;
        String limpio = valor.replace("\"", "\"\"");
        return requiereComillas ? '"' + limpio + '"' : limpio;
    }
}
