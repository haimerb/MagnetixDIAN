package com.magnetixdian.domain.dian;

public record ArchivoListadoDian(String nombreArchivo, byte[] contenido) {

    public ArchivoListadoDian {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            nombreArchivo = "listado-dian.xlsx";
        }
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El listado de la DIAN está vacío.");
        }
    }
}
