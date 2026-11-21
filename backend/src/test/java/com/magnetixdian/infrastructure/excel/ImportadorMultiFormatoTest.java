package com.magnetixdian.infrastructure.excel;

import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
import com.magnetixdian.interfaces.dto.FormatoDetectadoDto;
import com.magnetixdian.interfaces.dto.ImportacionMultiFormatoDto;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportadorMultiFormatoTest {

    private final ImportadorMultiFormato importador =
            new ImportadorMultiFormato(new FormatoExogenaRegistry());

    @Test
    void detectaFormatosYOmiteHojasSinCodigo() throws IOException {
        ImportacionMultiFormatoDto resultado = importador.importar(construirWorkbook());

        assertEquals(2, resultado.formatos().size());
        assertEquals(List.of("Observaciones"), resultado.hojasOmitidas());

        FormatoDetectadoDto formato1001 = resultado.formatos().get(0);
        assertEquals("1001", formato1001.codigo());
        assertTrue(formato1001.activo());
        assertEquals("ACTIVO", formato1001.estado());
        assertEquals(1, formato1001.filas());

        FormatoDetectadoDto formato1003 = resultado.formatos().get(1);
        assertEquals("1003", formato1003.codigo());
        assertFalse(formato1003.activo());
        assertEquals("PENDIENTE_XSD", formato1003.estado());
        assertEquals(1, formato1003.filas());
    }

    private byte[] construirWorkbook() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja1001 = workbook.createSheet("1001");
            fila(hoja1001, 0, "NIT", "VALOR_PAGO");
            fila(hoja1001, 1, "900123456", "1000");

            Sheet hoja1003 = workbook.createSheet("1003");
            fila(hoja1003, 0, "CONCEPTO", "VALOR");
            fila(hoja1003, 1, "C1003", "2000");

            Sheet observaciones = workbook.createSheet("Observaciones");
            fila(observaciones, 0, "Nota", "Texto");

            workbook.write(salida);
            return salida.toByteArray();
        }
    }

    private void fila(Sheet hoja, int indice, String... valores) {
        Row fila = hoja.createRow(indice);
        for (int i = 0; i < valores.length; i++) {
            fila.createCell(i).setCellValue(valores[i]);
        }
    }
}
