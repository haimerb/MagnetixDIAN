package com.magnetixdian.facturacion;

import com.magnetixdian.domain.factura.DatosAdquirente;
import com.magnetixdian.domain.factura.DatosEmisor;
import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.factura.TipoDocumento;
import com.magnetixdian.infrastructure.xml.GeneradorUblFactura;
import com.magnetixdian.interfaces.dto.NominaElectronicaDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorUblFacturaPruebaTest {

    private final GeneradorUblFactura generador = new GeneradorUblFactura();

    @Test
    void documentosDePruebaIncluyenTestSetIdYTipo() {
        String factura = texto(generador.construirDocumentoPrueba("FACTURA", "TESTID-100", 1));
        String notaDebito = texto(generador.construirDocumentoPrueba("NOTA_DEBITO", "TESTID-100", 2));
        String notaCredito = texto(generador.construirDocumentoPrueba("NOTA_CREDITO", "TESTID-100", 3));

        assertTrue(factura.contains("<Invoice"));
        assertTrue(factura.contains("<cbc:InvoiceTypeCode>01</cbc:InvoiceTypeCode>"));
        assertTrue(notaDebito.contains("<DebitNote"));
        assertTrue(notaDebito.contains("<cbc:DebitNoteTypeCode>92</cbc:DebitNoteTypeCode>"));
        assertTrue(notaCredito.contains("<CreditNote"));
        assertTrue(notaCredito.contains("<cbc:CreditNoteTypeCode>91</cbc:CreditNoteTypeCode>"));
        assertTrue(factura.contains("sts:TestSetId"));
        assertTrue(factura.contains("TESTID-100"));
    }

    @Test
    void documentoDePruebaRechazaTipoNoSoportado() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> generador.construirDocumentoPrueba("NOMINA", "TESTID-100", 1));
    }

    @Test
    void generarNominaIncluyeCodigo31YDetalleDelTrabajador() {
        NominaElectronicaDto nomina = new NominaElectronicaDto(
                "13", "12345678", "Ana Pérez", "2026-03", LocalDate.of(2026, 4, 5),
                List.of(new NominaElectronicaDto.ConceptoDto("SAL", "Salario", new BigDecimal("1000.00"))),
                List.of(new NominaElectronicaDto.ConceptoDto("AFP", "Aportes", new BigDecimal("100.00"))));
        String xml = generador.generarNomina(datosBase(), nomina, "CUFE-NOMINA-TEST");

        assertTrue(xml.contains("<cbc:InvoiceTypeCode>31</cbc:InvoiceTypeCode>"));
        assertTrue(xml.contains("Ana Pérez"));
        assertTrue(xml.contains("12345678"));
        assertTrue(xml.contains("SAL"));
        assertTrue(xml.contains("CUFE-NOMINA-TEST"));
        assertEquals(1, xml.split("Nomina Electronica", -1).length - 1);
    }

    private String texto(byte[] contenido) {
        return new String(contenido, StandardCharsets.UTF_8);
    }

    private FacturaDatos datosBase() {
        LineaFactura linea = LineaFactura.of(1, "Salario", BigDecimal.ONE, new BigDecimal("1000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, "94", null);
        return new FacturaDatos(TipoDocumento.FACTURA_VENTA, "TST31-001", "TST", 1,
                LocalDate.of(2026, 3, 30), LocalTime.of(10, 0), null, 2, "COP", "1", "10", "10",
                new DatosEmisor("31", "900123456", 4, "MAGNETIXDIAN SAS", "ORDINARIO", "R-99-PN",
                        "Calle 1", "Bogota", "Bogota", "facturacion@magnetixdian.com", false),
                new DatosAdquirente("13", "12345678", null, "Ana Pérez", null, null, null,
                        "ORDINARIO", "R-99-PN"),
                List.of(linea), List.of(), new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), null, null, null,
                "TEST-SOFTWARE", "TEST-PIN", "SECURITY", "TESTID-100", "RES-TEST",
                LocalDate.of(2026, 1, 1), LocalDate.of(2025, 12, 31), LocalDate.of(2026, 12, 31), 1, 1000);
    }
}
