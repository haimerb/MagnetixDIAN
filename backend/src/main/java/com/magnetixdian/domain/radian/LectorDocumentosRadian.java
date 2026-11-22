package com.magnetixdian.domain.radian;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.magnetixdian.interfaces.dto.DocumentoRadianImportDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianImpuestoDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianLineaDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LectorDocumentosRadian {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final DateTimeFormatter[] FORMATOS_FECHA = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    private LectorDocumentosRadian() {
    }

    public static List<DocumentoRadianImportDto> parsear(byte[] contenido, String nombreArchivo) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El archivo de documentos RADIAN está vacío.");
        }
        String nombre = nombreArchivo == null ? "" : nombreArchivo.toLowerCase(Locale.ROOT);
        String texto = new String(contenido, StandardCharsets.UTF_8).replace("\uFEFF", "").trim();
        if (texto.startsWith("[") || texto.startsWith("{") || nombre.endsWith(".json")) {
            return parsearJson(texto);
        }
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            return parsearExcel(contenido);
        }
        return parsearCsv(texto);
    }

    private static List<DocumentoRadianImportDto> parsearJson(String texto) {
        try {
            if (texto.startsWith("[")) {
                return MAPPER.readValue(texto, new TypeReference<List<DocumentoRadianImportDto>>() {
                });
            }
            return List.of(MAPPER.readValue(texto, DocumentoRadianImportDto.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON de documentos RADIAN inválido: " + ex.getOriginalMessage());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("No se pudo leer el JSON de documentos RADIAN.", ex);
        }
    }

    private static List<DocumentoRadianImportDto> parsearExcel(byte[] contenido) {
        List<DocumentoRadianImportDto> documentos = new ArrayList<>();
        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            if (libro.getNumberOfSheets() == 0) {
                return documentos;
            }
            Sheet hoja = libro.getSheetAt(0);
            Row cabecera = hoja.getRow(0);
            if (cabecera == null) {
                return documentos;
            }
            for (int f = 1; f <= hoja.getLastRowNum(); f++) {
                Row fila = hoja.getRow(f);
                if (fila == null || esVacia(fila)) {
                    continue;
                }
                Map<String, String> registro = new LinkedHashMap<>();
                for (int c = 0; c < cabecera.getLastCellNum(); c++) {
                    String clave = textoCelda(cabecera.getCell(c));
                    asignar(registro, clave, textoCelda(fila.getCell(c)));
                }
                documentos.add(desdeRegistro(registro));
            }
            return documentos;
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo XLSX de documentos RADIAN.", ex);
        }
    }

    private static List<DocumentoRadianImportDto> parsearCsv(String texto) {
        List<DocumentoRadianImportDto> documentos = new ArrayList<>();
        String[] filas = texto.split("\\r?\\n");
        if (filas.length == 0 || filas[0].isBlank()) {
            return documentos;
        }
        String separador = filas[0].indexOf(';') >= 0 ? ";" : ",";
        List<String> cabeceras = dividir(filas[0], separador);
        for (int i = 1; i < filas.length; i++) {
            if (filas[i].isBlank()) {
                continue;
            }
            List<String> valores = dividir(filas[i], separador);
            Map<String, String> registro = new LinkedHashMap<>();
            for (int c = 0; c < cabeceras.size(); c++) {
                String valor = c < valores.size() ? valores.get(c) : "";
                asignar(registro, cabeceras.get(c), valor.trim());
            }
            documentos.add(desdeRegistro(registro));
        }
        return documentos;
    }

    private static List<String> dividir(String fila, String separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < fila.length(); i++) {
            char caracter = fila.charAt(i);
            if (caracter == '"') {
                if (entreComillas && i + 1 < fila.length() && fila.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (separador.charAt(0) == caracter && !entreComillas) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(caracter);
            }
        }
        campos.add(actual.toString());
        return campos;
    }

    private static void asignar(Map<String, String> destino, String cabecera, String valor) {
        String clave = normalizar(cabecera);
        if (clave.isEmpty() || valor == null) {
            return;
        }
        switch (clave) {
            case "direccion" -> destino.put("direccion", valor);
            case "tipodocumento", "tipo", "tipodocumentoelectronico" -> destino.put("tipoDocumento", valor);
            case "cufecude", "cufe", "cude" -> destino.put("cufeCude", valor);
            case "consecutivo", "numero", "numerodocumento" -> destino.put("consecutivo", valor);
            case "nitcontraparte", "nit", "nitadquirente" -> destino.put("nitContraparte", valor);
            case "razonsocialcontraparte", "razonsocial", "nombrecontraparte" ->
                    destino.put("razonSocialContraparte", valor);
            case "fecha", "fechadocumento", "fechaemision" -> destino.put("fecha", valor);
            case "subtotal", "base" -> destino.put("subtotal", valor);
            case "impuestos", "totalimpuestos", "iva" -> destino.put("impuestos", valor);
            case "retenciones", "totalretenciones" -> destino.put("retenciones", valor);
            case "total", "totaldocumento", "valortotal" -> destino.put("total", valor);
            case "estadoradian", "estado" -> destino.put("estadoRadian", valor);
            default -> {
            }
        }
    }

    private static DocumentoRadianImportDto desdeRegistro(Map<String, String> registro) {
        return new DocumentoRadianImportDto(
                null,
                registro.get("direccion"),
                registro.get("tipoDocumento"),
                registro.get("cufeCude"),
                registro.get("consecutivo"),
                registro.get("nitContraparte"),
                registro.get("razonSocialContraparte"),
                fecha(registro.get("fecha")),
                numero(registro.get("subtotal")),
                numero(registro.get("impuestos")),
                numero(registro.get("retenciones")),
                numero(registro.get("total")),
                registro.get("estadoRadian"),
                List.<DocumentoRadianLineaDto>of(),
                List.<DocumentoRadianImpuestoDto>of());
    }

    private static String textoCelda(Cell celda) {
        if (celda == null) {
            return "";
        }
        return switch (celda.getCellType()) {
            case STRING -> celda.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(celda)) {
                    yield celda.getDateCellValue().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
                }
                yield BigDecimal.valueOf(celda.getNumericCellValue()).toPlainString();
            }
            case BOOLEAN -> String.valueOf(celda.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield BigDecimal.valueOf(celda.getNumericCellValue()).toPlainString();
                } catch (RuntimeException ex) {
                    yield celda.getStringCellValue().trim();
                }
            }
            default -> "";
        };
    }

    private static boolean esVacia(Row fila) {
        for (int c = 0; c < fila.getLastCellNum(); c++) {
            Cell celda = fila.getCell(c);
            if (celda != null && celda.getCellType() != CellType.BLANK
                    && !textoCelda(celda).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(valor.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.replaceAll("[^a-z0-9]", "");
    }

    private static LocalDate fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.trim();
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (RuntimeException ex) {
                continue;
            }
        }
        throw new IllegalArgumentException("Fecha de documento RADIAN inválida: " + valor);
    }

    private static BigDecimal numero(String valor) {
        if (valor == null || valor.isBlank()) {
            return BigDecimal.ZERO;
        }
        String texto = valor.replace("$", "").replace("COP", "").trim();
        if (texto.contains(",") && texto.contains(".")) {
            texto = texto.lastIndexOf(',') > texto.lastIndexOf('.')
                    ? texto.replace(".", "").replace(",", ".")
                    : texto.replace(",", "");
        } else if (texto.contains(",")) {
            String[] partes = texto.split(",");
            texto = partes.length == 2 && partes[1].length() <= 2
                    ? partes[0].replace(".", "") + "." + partes[1]
                    : texto.replace(",", "");
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor numérico inválido: " + valor);
        }
    }
}
