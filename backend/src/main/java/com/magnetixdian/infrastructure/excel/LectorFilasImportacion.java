package com.magnetixdian.infrastructure.excel;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lee un archivo tabular (CSV o XLSX) y devuelve sus filas como texto.
 * Para CSV detecta el separador (coma o punto y coma) y soporta campos entre comillas.
 */
public final class LectorFilasImportacion {

    private LectorFilasImportacion() {
    }

    public static List<List<String>> leer(byte[] contenido, String nombreArchivo) {
        boolean xlsx = (nombreArchivo != null && nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".xlsx"))
                || esZip(contenido);
        return xlsx ? leerXlsx(contenido) : leerCsv(contenido);
    }

    private static boolean esZip(byte[] contenido) {
        return contenido.length > 1 && contenido[0] == 'P' && contenido[1] == 'K';
    }

    public static List<List<String>> leerCsv(byte[] contenido) {
        String texto = new String(contenido, StandardCharsets.UTF_8);
        if (!texto.isEmpty() && texto.charAt(0) == '\uFEFF') {
            texto = texto.substring(1);
        }
        List<String> lineas = partirLineas(texto);
        if (lineas.isEmpty()) {
            return List.of();
        }
        char separador = detectarSeparador(lineas.get(0));
        List<List<String>> filas = new ArrayList<>();
        for (String linea : lineas) {
            if (linea.isBlank()) {
                continue;
            }
            filas.add(partirCampos(linea, separador));
        }
        return filas;
    }

    private static char detectarSeparador(String linea) {
        int puntoYComa = 0;
        int coma = 0;
        boolean entreComillas = false;
        for (char c : linea.toCharArray()) {
            if (c == '"') {
                entreComillas = !entreComillas;
            } else if (!entreComillas && c == ';') {
                puntoYComa++;
            } else if (!entreComillas && c == ',') {
                coma++;
            }
        }
        return puntoYComa >= coma ? ';' : ',';
    }

    private static List<String> partirLineas(String texto) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '"') {
                entreComillas = !entreComillas;
                actual.append(c);
            } else if ((c == '\n' || c == '\r') && !entreComillas) {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                lineas.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        if (!actual.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private static List<String> partirCampos(String linea, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == separador && !entreComillas) {
                campos.add(actual.toString().trim());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString().trim());
        return campos;
    }

    private static List<List<String>> leerXlsx(byte[] contenido) {
        List<List<String>> filas = new ArrayList<>();
        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter formato = new DataFormatter();
            for (Row fila : hoja) {
                List<String> celdas = new ArrayList<>();
                int ultima = Math.max(fila.getLastCellNum(), 0);
                for (int i = 0; i < ultima; i++) {
                    celdas.add(formato.formatCellValue(fila.getCell(i)).trim());
                }
                if (celdas.stream().anyMatch(c -> !c.isBlank())) {
                    filas.add(celdas);
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo Excel: " + ex.getMessage(), ex);
        }
        return filas;
    }
}
