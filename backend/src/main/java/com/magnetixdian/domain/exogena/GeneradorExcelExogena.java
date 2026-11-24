package com.magnetixdian.domain.exogena;

import com.magnetixdian.interfaces.dto.ExogenaRenglonDto;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class GeneradorExcelExogena {

    public byte[] generar(String formato, List<ExogenaRenglonDto> renglones) {
        String[] cabeceras = {"AÑO", "FORMATO", "TERCERO_ID", "TIPO_DOCUMENTO", "NUMERO_DOCUMENTO",
                "DV", "RAZON_SOCIAL", "MUNICIPIO", "PAIS", "CONCEPTO", "DESCRIPCION_CONCEPTO",
                "VALOR", "CANTIDAD_MOVIMIENTOS"};
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Formato " + formato);
            Row filaCabecera = hoja.createRow(0);
            for (int i = 0; i < cabeceras.length; i++) {
                filaCabecera.createCell(i).setCellValue(cabeceras[i]);
            }
            int numeroFila = 1;
            if (renglones != null) {
                for (ExogenaRenglonDto renglon : renglones) {
                    Row fila = hoja.createRow(numeroFila++);
                    fila.createCell(0).setCellValue(renglon.ano() == null ? "" : renglon.ano().toString());
                    fila.createCell(1).setCellValue(renglon.formato() == null ? "" : renglon.formato());
                    fila.createCell(2).setCellValue(renglon.terceroId() == null ? "" : renglon.terceroId().toString());
                    fila.createCell(3).setCellValue(nulo(renglon.tipoDocumento()));
                    fila.createCell(4).setCellValue(nulo(renglon.numeroDocumento()));
                    fila.createCell(5).setCellValue(nulo(renglon.digitoVerificacion()));
                    fila.createCell(6).setCellValue(nulo(renglon.razonSocial()));
                    fila.createCell(7).setCellValue(nulo(renglon.municipio()));
                    fila.createCell(8).setCellValue(nulo(renglon.pais()));
                    fila.createCell(9).setCellValue(nulo(renglon.codigoConcepto()));
                    fila.createCell(10).setCellValue(nulo(renglon.descripcionConcepto()));
                    fila.createCell(11).setCellValue(renglon.valor() == null ? 0 : renglon.valor().doubleValue());
                    fila.createCell(12).setCellValue(renglon.cantidadMovimientos() == null ? 0 : renglon.cantidadMovimientos());
                }
            }
            for (int i = 0; i < cabeceras.length; i++) {
                hoja.autoSizeColumn(i);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar el archivo Excel de exógena", ex);
        }
    }

    private String nulo(String valor) {
        return valor == null ? "" : valor;
    }
}
