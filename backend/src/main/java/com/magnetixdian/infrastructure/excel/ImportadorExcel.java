package com.magnetixdian.infrastructure.excel;

import com.magnetixdian.domain.model.OperacionDatos;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Importador de datos desde la plantilla Excel del prevalidador DIAN
 * (formato 1001) o desde un archivo con la siguiente estructura por fila:
 *
 * <pre>
 *   A: tipo documento    (NIT | CC | NITCE | …)
 *   B: NIT / documento   (sin DV)
 *   C: DV                (solo NIT)
 *   D: concepto           (código concepto DIAN)
 *   E: valor pago
 *   F: retención renta
 *   G: retención IVA
 *   H: retención ICA
 *   I: retención timbre
 *   J: IVA pagado
 *   K: IVA descontable
 *   L: valor gasto
 *   M: valor costo
 *   N: valor nota crédito
 * </pre>
 *
 * <p>Si la primera fila tiene encabezados se detecta y se omite; las filas en
 * blanco se ignoran.</p>
 */
@Service
public class ImportadorExcel {

    private static final int COL_TIPO           = 0;
    private static final int COL_DOCUMENTO      = 1;
    private static final int COL_DV             = 2;
    private static final int COL_CONCEPTO       = 3;
    private static final int COL_VALOR_PAGO     = 4;
    private static final int COL_RET_RENTA      = 5;
    private static final int COL_RET_IVA        = 6;
    private static final int COL_RET_ICA        = 7;
    private static final int COL_RET_TIMBRE     = 8;
    private static final int COL_IVA_PAGADO     = 9;
    private static final int COL_IVA_DESC       = 10;
    private static final int COL_VALOR_GASTO    = 11;
    private static final int COL_VALOR_COSTO    = 12;
    private static final int COL_VALOR_NC       = 13;

    public List<OperacionDatos> importar(MultipartFile archivo) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            return leerHoja(sheet);
        }
    }

    public List<OperacionDatos> importar(byte[] contenido) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(contenido))) {
            return leerHoja(workbook.getSheetAt(0));
        }
    }

    private List<OperacionDatos> leerHoja(Sheet sheet) {
        List<OperacionDatos> operaciones = new ArrayList<>();
        int inicio = detectarInicio(sheet);

        for (int i = inicio; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (esFilaVacia(row)) {
                continue;
            }
            OperacionDatos op = mapearFila(row, i + 1);
            if (op != null) {
                operaciones.add(op);
            }
        }
        return operaciones;
    }

    /**
     * Detecta la primera fila de datos: omite filas de título/encabezado
     * verificando si la columna A contiene un tipo de documento conocido.
     */
    private int detectarInicio(Sheet sheet) {
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 10); i++) {
            Row row = sheet.getRow(i);
            String tipoDoc = celdaTexto(row, COL_TIPO);
            if (tipoDoc != null && !tipoDoc.isBlank() && esTipoDocumento(tipoDoc)) {
                return i;
            }
        }
        return 0;
    }

    private boolean esTipoDocumento(String valor) {
        return valor.equalsIgnoreCase("NIT")
                || valor.equalsIgnoreCase("CC")
                || valor.equalsIgnoreCase("CE")
                || valor.equalsIgnoreCase("PASAPORTE")
                || valor.equalsIgnoreCase("NITCE")
                || valor.equalsIgnoreCase("RUT");
    }

    private OperacionDatos mapearFila(Row row, int linea) {
        String tipo = celdaTexto(row, COL_TIPO);
        String documento = celdaTexto(row, COL_DOCUMENTO);
        if ((tipo == null || tipo.isBlank()) && (documento == null || documento.isBlank())) {
            return null;
        }
        return new OperacionDatos(
                linea,
                tipo,
                documento,
                numeroEntero(row, COL_DV),
                celdaTexto(row, COL_CONCEPTO),
                numero(row, COL_VALOR_PAGO),
                numero(row, COL_RET_RENTA),
                numero(row, COL_RET_IVA),
                numero(row, COL_RET_ICA),
                numero(row, COL_RET_TIMBRE),
                numero(row, COL_IVA_PAGADO),
                numero(row, COL_IVA_DESC),
                numero(row, COL_VALOR_GASTO),
                numero(row, COL_VALOR_COSTO),
                numero(row, COL_VALOR_NC),
                false);
    }

    private boolean esFilaVacia(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c <= COL_VALOR_NC; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String texto = cell.getCellType() == CellType.FORMULA
                        ? String.valueOf(cell.getNumericCellValue())
                        : cell.toString();
                if (texto != null && !texto.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String celdaTexto(Row row, int col) {
        Cell cell = row != null ? row.getCell(col) : null;
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue())
                    .replace(".0", "").trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue()).replace(".0", "").trim();
                } catch (RuntimeException e) {
                    yield cell.getStringCellValue().trim();
                }
            }
            default -> null;
        };
    }

    private BigDecimal numero(Row row, int col) {
        Cell cell = row != null ? row.getCell(col) : null;
        if (cell == null) {
            return null;
        }
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> {
                    String valor = cell.getStringCellValue().trim().replace(",", "");
                    yield valor.isBlank() ? null : new BigDecimal(valor);
                }
                case FORMULA -> BigDecimal.valueOf(cell.getNumericCellValue());
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Integer numeroEntero(Row row, int col) {
        BigDecimal valor = numero(row, col);
        return valor == null ? null : valor.intValue();
    }
}