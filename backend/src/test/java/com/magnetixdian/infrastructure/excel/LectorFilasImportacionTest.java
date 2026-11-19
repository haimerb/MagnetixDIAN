package com.magnetixdian.infrastructure.excel;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LectorFilasImportacionTest {

    private static byte[] utf8(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void detectaPuntoYComaYRespetaComillas() {
        List<List<String>> filas = LectorFilasImportacion.leerCsv(utf8(
                "grupo;descripcion;valor\n"
                        + "A;\"Servicio \"\"premium\"\"\";100\n"
                        + "A;Cafe;50\n"));

        assertThat(filas).hasSize(3);
        assertThat(filas.get(1)).containsExactly("A", "Servicio \"premium\"", "100");
        assertThat(filas.get(2)).containsExactly("A", "Cafe", "50");
    }

    @Test
    void detectaComaYQuitaBom() {
        List<List<String>> filas = LectorFilasImportacion.leerCsv(utf8(
                "\uFEFFgrupo,descripcion,valor\nA,Servicio,100\n"));

        assertThat(filas).hasSize(2);
        assertThat(filas.get(0)).containsExactly("grupo", "descripcion", "valor");
        assertThat(filas.get(1)).containsExactly("A", "Servicio", "100");
    }

    @Test
    void soportaSaltoDeLineaDentroDeComillas() {
        List<List<String>> filas = LectorFilasImportacion.leerCsv(utf8(
                "grupo;descripcion\nA;\"linea uno\nlinea dos\"\n"));

        assertThat(filas).hasSize(2);
        assertThat(filas.get(1)).containsExactly("A", "linea uno\nlinea dos");
    }

    @Test
    void omiteLineasVacias() {
        List<List<String>> filas = LectorFilasImportacion.leerCsv(utf8(
                "grupo;descripcion\n\nA;Servicio\n\n"));

        assertThat(filas).hasSize(2);
    }
}
