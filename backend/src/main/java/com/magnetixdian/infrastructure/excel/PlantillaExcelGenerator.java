package com.magnetixdian.infrastructure.excel;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Genera la plantilla Excel editable del formato 1001/1002: una hoja listada
 * con encabezados y dos filas de ejemplo que el importador detecta y omite.
 */
@Component
public class PlantillaExcelGenerator {

    private static final String[] ENCABEZADOS = {
            "TIPO", "NIT", "DV", "CONCEPTO", "VALOR_PAGO", "RET_RENTA",
            "RET_IVA", "RET_ICA", "RET_TIMBRE", "IVA_PAGADO", "IVA_DESC",
            "VALOR_GASTO", "VALOR_COSTO", "NOTA_CREDITO"
    };

    private static final int COL_MARCA_EJEMPLO = 14;

    public byte[] generar(String formato) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(formato);
            sheet.setColumnWidth(0, 10);
            for (int c = 1; c < ENCABEZADOS.length; c++) {
                sheet.setColumnWidth(c, 16);
            }

            Row titulo = sheet.createRow(0);
            titulo.createCell(0).setCellValue("MagnetixDIAN — Plantilla formato " + formato + " (información exógena DIAN)");

            Row header = sheet.createRow(1);
            CellStyle estiloHeader = headerStyle(wb);
            for (int c = 0; c < ENCABEZADOS.length; c++) {
                var cell = header.createCell(c);
                cell.setCellValue(ENCABEZADOS[c]);
                cell.setCellStyle(estiloHeader);
            }
            var marcaHeader = header.createCell(COL_MARCA_EJEMPLO);
            marcaHeader.setCellValue("EJEMPLO");
            marcaHeader.setCellStyle(estiloHeader);

            ejemplos(sheet);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void ejemplos(Sheet sheet) {
        Row ejemplo1 = sheet.createRow(2);
        ejemplo1.createCell(0).setCellValue("NIT");
        ejemplo1.createCell(1).setCellValue("900123456");
        ejemplo1.createCell(2).setCellValue(8);
        ejemplo1.createCell(3).setCellValue("2012");
        ejemplo1.createCell(4).setCellValue(3000000.0);
        ejemplo1.createCell(5).setCellValue(100000.0);
        ejemplo1.createCell(COL_MARCA_EJEMPLO).setCellValue("EJEMPLO");

        Row ejemplo2 = sheet.createRow(3);
        ejemplo2.createCell(0).setCellValue("CC");
        ejemplo2.createCell(1).setCellValue("1020304050");
        ejemplo2.createCell(3).setCellValue("2005");
        ejemplo2.createCell(4).setCellValue(800000.0);
        ejemplo2.createCell(COL_MARCA_EJEMPLO).setCellValue("EJEMPLO");
    }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}