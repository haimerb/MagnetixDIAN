package com.magnetixdian.infrastructure.excel;

import com.magnetixdian.domain.exogena.FormatoExogena;
import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
import com.magnetixdian.interfaces.dto.FormatoDetectadoDto;
import com.magnetixdian.interfaces.dto.ImportacionMultiFormatoDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImportadorMultiFormato {

    private static final Pattern CODIGO_FORMATO = Pattern.compile("(^|\\D)(\\d{4})(\\D|$)");
    private static final int MAX_FILAS = 10_000;

    private final FormatoExogenaRegistry formatos;

    public ImportadorMultiFormato(FormatoExogenaRegistry formatos) {
        this.formatos = formatos;
    }

    public ImportacionMultiFormatoDto importar(MultipartFile archivo) throws IOException {
        return importar(archivo.getBytes());
    }

    public ImportacionMultiFormatoDto importar(byte[] contenido) throws IOException {
        List<FormatoDetectadoDto> detectados = new ArrayList<>();
        List<String> omitidas = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet hoja = workbook.getSheetAt(i);
                String codigo = extraerCodigo(hoja.getSheetName());
                if (codigo == null) {
                    omitidas.add(hoja.getSheetName());
                    continue;
                }
                detectados.add(leerHoja(hoja, codigo));
            }
        }

        return new ImportacionMultiFormatoDto(null, detectados, omitidas, 0);
    }

    private FormatoDetectadoDto leerHoja(Sheet hoja, String codigo) {
        List<Row> filas = new ArrayList<>();
        for (int i = 0; i <= hoja.getLastRowNum() && filas.size() < MAX_FILAS; i++) {
            Row fila = hoja.getRow(i);
            if (!esFilaVacia(fila)) {
                filas.add(fila);
            }
        }

        if (filas.isEmpty()) {
            return construir(codigo, hoja.getSheetName(), List.of(), List.of());
        }

        boolean tieneEncabezado = tieneEncabezado(filas.get(0));
        List<String> encabezados = titulos(filas.get(0), tieneEncabezado);
        int inicio = tieneEncabezado ? 1 : 0;
        List<Map<String, String>> datos = new ArrayList<>();

        for (int i = inicio; i < filas.size(); i++) {
            Row fila = filas.get(i);
            Map<String, String> registro = new LinkedHashMap<>();
            for (int c = 0; c < encabezados.size(); c++) {
                registro.put(encabezados.get(c), texto(fila, c));
            }
            datos.add(registro);
        }

        return construir(codigo, hoja.getSheetName(), encabezados, datos);
    }

    private FormatoDetectadoDto construir(String codigo, String hoja,
                                          List<String> encabezados,
                                          List<Map<String, String>> datos) {
        FormatoExogena formato = formatos.existe(codigo) ? formatos.obtener(codigo) : null;
        String estado = formato == null ? "DESCONOCIDO" : formato.estado();
        boolean activo = formato != null && formato.activo();
        return new FormatoDetectadoDto(
                codigo, hoja, estado, activo, datos.size(), encabezados, datos, null);
    }

    private String extraerCodigo(String nombreHoja) {
        if (nombreHoja == null) {
            return null;
        }
        Matcher matcher = CODIGO_FORMATO.matcher(nombreHoja);
        return matcher.find() ? matcher.group(2) : null;
    }

    private boolean tieneEncabezado(Row fila) {
        int textos = 0;
        for (int c = 0; c < fila.getLastCellNum(); c++) {
            Cell celda = fila.getCell(c);
            if (celda != null && celda.getCellType() == CellType.STRING && !celda.getStringCellValue().isBlank()) {
                textos++;
            }
        }
        return textos >= 2;
    }

    private List<String> titulos(Row fila, boolean tieneEncabezado) {
        int columnas = Math.max(fila.getLastCellNum(), 1);
        List<String> encabezados = new ArrayList<>();
        for (int c = 0; c < columnas; c++) {
            String valor = tieneEncabezado ? texto(fila, c) : null;
            encabezados.add(valor == null || valor.isBlank() ? "COLUMNA_" + (c + 1) : valor);
        }
        return encabezados;
    }

    private boolean esFilaVacia(Row fila) {
        if (fila == null) {
            return true;
        }
        for (int c = 0; c < fila.getLastCellNum(); c++) {
            String valor = texto(fila, c);
            if (valor != null && !valor.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String texto(Row fila, int columna) {
        Cell celda = fila == null ? null : fila.getCell(columna);
        if (celda == null) {
            return null;
        }
        return switch (celda.getCellType()) {
            case STRING -> celda.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(celda.getNumericCellValue())
                    .replace(".0", "").trim();
            case BOOLEAN -> String.valueOf(celda.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(celda.getNumericCellValue()).replace(".0", "").trim();
                } catch (RuntimeException e) {
                    yield celda.getStringCellValue().trim();
                }
            }
            default -> null;
        };
    }
}
