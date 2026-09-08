package com.magnetixdian.infrastructure.excel;

import com.magnetixdian.domain.model.OperacionDatos;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportadorExcelTest {

    private final ImportadorExcel importador = new ImportadorExcel();

    @Test
    void importaDatosSaltandoTituloEncabezadoYFilasVacias() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("1001");
            sheet.createRow(0).createCell(0).setCellValue("PREVALIDADOR DIAN 1001");
            Row header = sheet.createRow(1);
            String[] titulos = {"TIPO", "NIT", "DV", "CONCEPTO", "VALOR_PAGO", "RET_RENTA",
                    "RET_IVA", "RET_ICA", "RET_TIMBRE", "IVA_PAGADO", "IVA_DESC",
                    "VALOR_GASTO", "VALOR_COSTO", "NC"};
            for (int c = 0; c < titulos.length; c++) {
                header.createCell(c).setCellValue(titulos[c]);
            }
            Row fila1 = sheet.createRow(2);
            fila1.createCell(0).setCellValue("NIT");
            fila1.createCell(1).setCellValue("900123456");
            fila1.createCell(2).setCellValue(8);
            fila1.createCell(3).setCellValue("2012");
            fila1.createCell(4).setCellValue(3000000.0);
            fila1.createCell(5).setCellValue(100000.0);

            Row fila2 = sheet.createRow(3);
            fila2.createCell(0).setCellValue("CC");
            fila2.createCell(1).setCellValue("1020304050");
            fila2.createCell(3).setCellValue("2005");
            fila2.createCell(4).setCellValue(800000.0);

            Row vacia = sheet.createRow(4);
            for (int c = 0; c < 14; c++) {
                vacia.createCell(c).setBlank();
            }

            List<OperacionDatos> ops = importador.importar(guardar(wb));

            assertEquals(2, ops.size());
            OperacionDatos op1 = ops.get(0);
            assertEquals(3, op1.linea());
            assertEquals("NIT", op1.tipoDocumento());
            assertEquals("900123456", op1.numeroIdentificacion());
            assertEquals(8, op1.dv());
            assertEquals("2012", op1.concepto());
            assertEquals(0, op1.valorPago().compareTo(new BigDecimal("3000000")));
            assertEquals(0, op1.retencionRenta().compareTo(new BigDecimal("100000")));
            assertNull(op1.retencionIva());

            OperacionDatos op2 = ops.get(1);
            assertEquals("CC", op2.tipoDocumento());
            assertNull(op2.dv());
            assertNull(op2.retencionRenta());
        }
    }

    @Test
    void archivoVacioDevuelveListaVacia() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("1001");
            assertTrue(importador.importar(guardar(wb)).isEmpty());
        }
    }

    @Test
    void filaConDocumentoPeroSinTipoSeImporta() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("1001");
            Row fila = sheet.createRow(0);
            fila.createCell(1).setCellValue("900123456");
            fila.createCell(3).setCellValue("2012");
            fila.createCell(4).setCellValue(500000.0);

            List<OperacionDatos> ops = importador.importar(guardar(wb));
            assertEquals(1, ops.size());
            assertNull(ops.get(0).tipoDocumento());
            assertEquals("900123456", ops.get(0).numeroIdentificacion());
        }
    }

    @Test
    void filasDeEjemploMarcadasConEjemploSeOmiten() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("1001");
            sheet.createRow(0).createCell(0).setCellValue("MagnetixDIAN — Plantilla formato 1001");
            Row header = sheet.createRow(1);
            String[] titulos = {"TIPO", "NIT", "DV", "CONCEPTO", "VALOR_PAGO", "RET_RENTA",
                    "RET_IVA", "RET_ICA", "RET_TIMBRE", "IVA_PAGADO", "IVA_DESC",
                    "VALOR_GASTO", "VALOR_COSTO", "NOTA_CREDITO", "EJEMPLO"};
            for (int c = 0; c < titulos.length; c++) {
                header.createCell(c).setCellValue(titulos[c]);
            }

            Row ejemplo = sheet.createRow(2);
            ejemplo.createCell(0).setCellValue("NIT");
            ejemplo.createCell(1).setCellValue("900123456");
            ejemplo.createCell(4).setCellValue(3000000.0);
            ejemplo.createCell(14).setCellValue("EJEMPLO");

            Row real = sheet.createRow(3);
            real.createCell(0).setCellValue("NIT");
            real.createCell(1).setCellValue("830111122");
            real.createCell(3).setCellValue("2005");
            real.createCell(4).setCellValue(12000000.0);

            List<OperacionDatos> ops = importador.importar(guardar(wb));

            assertEquals(1, ops.size());
            assertEquals("830111122", ops.get(0).numeroIdentificacion());
        }
    }

    private byte[] guardar(XSSFWorkbook wb) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        }
    }
}