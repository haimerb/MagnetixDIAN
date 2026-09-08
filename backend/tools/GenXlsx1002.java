import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.FileOutputStream;

/**
 * Genera target/ejemplo-1002.xlsx (formato 1002): 3 registros de créditos,
 * descuentos y notas de ajuste SIN errores. DV correctos según módulo 11:
 * 900123456 -> 8 | 830111122 -> 8 | 1015678942 -> 9.
 */
public class GenXlsx1002 {
    public static void main(String[] a) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Formato1002");
            Object[][] rows = {
                {"NIT", "900123456", 8, "7001", 2500000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2500000.0},
                {"NIT", "830111122", 8, "7002", 800000.0, 80000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
                {"NIT", "1015678942", 9, "7005", 1200000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1200000.0},
            };
            int r = 0;
            for (Object[] row : rows) {
                Row excelRow = sheet.createRow(r);
                for (int c = 0; c < row.length; c++) {
                    Object v = row[c];
                    if (v == null) continue;
                    if (v instanceof String s) excelRow.createCell(c).setCellValue(s);
                    else if (v instanceof Double d) excelRow.createCell(c).setCellValue(d);
                    else if (v instanceof Integer i) excelRow.createCell(c).setCellValue(i.doubleValue());
                    else excelRow.createCell(c).setCellValue(String.valueOf(v));
                }
                r++;
            }
            FileOutputStream out = new FileOutputStream("target/ejemplo-1002.xlsx");
            wb.write(out);
            out.close();
            System.out.println("OK: ejemplo-1002.xlsx");
        }
    }
}
