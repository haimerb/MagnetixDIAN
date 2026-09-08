import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.FileOutputStream;

/**
 * Genera target/ejemplo-1001-limpio.xlsx: 5 registros SIN errores de
 * validación (conceptos del catálogo, DV correctos, montos ≥ 3 UVT y no
 * negativos). Usado para smoke test de flujo completo (0 errores → XML).
 * Incluye fila de encabezados como la plantilla (el importador la omite).
 *
 * DV según módulo 11 DIAN (calculados con la misma regla del backend):
 *   900123456 -> 8 | 830111122 -> 8 | 1015678942 -> 9
 *   900111111 -> 0 | 901111111 -> 4
 */
public class GenXlsxLimpio {
    public static void main(String[] a) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Formato1001");
            String[] header = {"TIPO_DOC", "NUMERO_DOC", "DV", "CONCEPTO", "VALOR_PAGO",
                "RET_RENTA", "RET_IVA", "RET_ICA", "RET_TIMBRE", "IVA_PAGADO", "IVA_DESC", "GASTO", "COSTO", "NOTA_CREDITO"};
            Object[][] rows = {
                {"NIT", "900123456", 8, "2012", 3000000.0, 300000.0, 0.0, 0.0, 0.0, 570000.0, 0.0, 3000000.0, 0.0, 0.0},
                {"NIT", "830111122", 8, "2005", 12000000.0, 1320000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12000000.0, 0.0, 0.0},
                {"NIT", "1015678942", 9, "2009", 5000000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5000000.0, 0.0, 0.0},
                {"NIT", "900111111", 0, "2004", 7000000.0, 700000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7000000.0, 0.0, 0.0},
                {"NIT", "901111111", 4, "2050", 6000000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6000000.0, 0.0, 0.0},
            };
            int r = 0;
            Row headerRow = sheet.createRow(r++);
            for (int c = 0; c < header.length; c++) headerRow.createCell(c).setCellValue(header[c]);
            for (Object[] row : rows) {
                Row excelRow = sheet.createRow(r++);
                for (int c = 0; c < row.length; c++) {
                    Object v = row[c];
                    if (v == null) continue;
                    if (v instanceof String s) excelRow.createCell(c).setCellValue(s);
                    else if (v instanceof Double d) excelRow.createCell(c).setCellValue(d);
                    else if (v instanceof Integer i) excelRow.createCell(c).setCellValue(i.doubleValue());
                    else excelRow.createCell(c).setCellValue(String.valueOf(v));
                }
            }
            FileOutputStream out = new FileOutputStream("target/ejemplo-1001-limpio.xlsx");
            wb.write(out);
            out.close();
            System.out.println("OK: ejemplo-1001-limpio.xlsx");
        }
    }
}
