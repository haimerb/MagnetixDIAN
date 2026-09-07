import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.FileOutputStream;

public class GenXlsx {
    public static void main(String[] a) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Formato1001");
            String[] header = {"TIPO_DOC", "NUMERO_DOC", "DV", "CONCEPTO", "VALOR_PAGO",
                "RET_RENTA", "RET_IVA", "RET_ICA", "RET_TIMBRE", "IVA_PAGADO", "IVA_DESC", "GASTO", "COSTO", "NOTA_CREDITO"};
            Row h = sheet.createRow(0);
            for (int i = 0; i < header.length; i++) h.createCell(i).setCellValue(header[i]);
            Object[][] rows = {
                {"NIT", "900123456", 1, "2004", 5000000.0, 550000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5000000.0, 0.0, 0.0},
                {"NIT", "830111122", 5, "2005", 12000000.0, 1320000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12000000.0, 0.0, 0.0},
                {"CC", "1015678942", null, "2009", 800000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 800000.0, 0.0, 0.0},
                {"NIT", "99999", 9, "9999", 100000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 100000.0, 0.0, 0.0},
                {"NIT", "900123456", 1, "2004", -5000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            };
            int r = 1;
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
            FileOutputStream out = new FileOutputStream("target/ejemplo-1001.xlsx");
            wb.write(out);
            out.close();
            System.out.println("OK: ejemplo-1001.xlsx");
        }
    }
}
