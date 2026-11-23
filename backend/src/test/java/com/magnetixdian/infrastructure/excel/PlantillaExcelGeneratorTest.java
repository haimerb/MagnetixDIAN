package com.magnetixdian.infrastructure.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlantillaExcelGeneratorTest {

    private final PlantillaExcelGenerator generator = new PlantillaExcelGenerator();

    @Test
    void generaPlantillaBasicaConEncabezadosYFilasDeEjemplo() throws IOException {
        byte[] archivo = generator.generar("1001");

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            assertEquals("1001", libro.getSheetName(0));
            Sheet hoja = libro.getSheetAt(0);
            assertEquals("MagnetixDIAN — Plantilla formato 1001 (información exógena DIAN)",
                    hoja.getRow(0).getCell(0).getStringCellValue());

            Row cabecera = hoja.getRow(1);
            assertEquals("TIPO", cabecera.getCell(0).getStringCellValue());
            assertEquals("NOTA_CREDITO", cabecera.getCell(13).getStringCellValue());
            assertEquals("EJEMPLO", cabecera.getCell(14).getStringCellValue());

            assertEquals("EJEMPLO", hoja.getRow(2).getCell(14).getStringCellValue());
            assertEquals("NIT", hoja.getRow(2).getCell(0).getStringCellValue());
            assertEquals("EJEMPLO", hoja.getRow(3).getCell(14).getStringCellValue());
            assertEquals("CC", hoja.getRow(3).getCell(0).getStringCellValue());
        }
    }

    @Test
    void generaPlantillaConEncabezadosOficialesSinFilasDeEjemplo() throws IOException {
        byte[] archivo = generator.generar("1125", List.of("TIPO_IDENTIFICACION", "NUMERO_IDENTIFICACION"));

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = libro.getSheetAt(0);
            assertEquals("1125", hoja.getSheetName());
            assertEquals("MagnetixDIAN — Plantilla oficial formato 1125 (información exógena DIAN)",
                    hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("TIPO_IDENTIFICACION", hoja.getRow(1).getCell(0).getStringCellValue());
            assertEquals("NUMERO_IDENTIFICACION", hoja.getRow(1).getCell(1).getStringCellValue());
            assertNull(hoja.getRow(2));
        }
    }

    @Test
    void generaPlantillaBasicaCuandoEncabezadosEstanVacios() throws IOException {
        byte[] archivo = generator.generar("1002", List.of());

        assertNotNull(archivo);
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = libro.getSheetAt(0);
            assertEquals("1002", hoja.getSheetName());
            assertEquals("TIPO", hoja.getRow(1).getCell(0).getStringCellValue());
        }
    }
}
