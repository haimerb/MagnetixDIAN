package com.magnetixdian.domain.radian;

import com.magnetixdian.interfaces.dto.DocumentoRadianImportDto;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LectorDocumentosRadianZipTest {

    @Test
    void parsearZipDeListadosDianConEncabezadosOficiales() throws IOException {
        String csv = "NIT Receptor;Razón Social Contraparte;Fecha de Emisión;Prefijo;Folio;"
                + "Valor total;Estado RADIAN;Tipo Documento\n"
                + "900123456;Empresa Demo;15/01/2026;F;002;1190,00;RECIBIDO;01\n";
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            zip.putNextEntry(new ZipEntry("listado.csv"));
            zip.write(csv.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        List<DocumentoRadianImportDto> documentos = LectorDocumentosRadian.parsear(
                salida.toByteArray(), "listados-dian.zip");

        assertEquals(1, documentos.size());
        DocumentoRadianImportDto documento = documentos.get(0);
        assertEquals("900123456", documento.nitContraparte());
        assertEquals("Empresa Demo", documento.razonSocialContraparte());
        assertEquals(LocalDate.of(2026, 1, 15), documento.fecha());
        assertEquals("F-002", documento.consecutivo());
        assertEquals("01", documento.tipoDocumento());
    }
}
