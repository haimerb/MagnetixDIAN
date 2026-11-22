package com.magnetixdian.infrastructure.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.magnetixdian.domain.factura.DatosAdquirente;
import com.magnetixdian.domain.factura.DatosEmisor;
import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.factura.TipoDocumento;
import com.magnetixdian.interfaces.dto.BrandingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneradorPdfFacturaBrandingTest {

    private GeneradorPdfFactura generador;
    private FacturaDatos factura;

    @BeforeEach
    void setUp() {
        generador = new GeneradorPdfFactura();
        DatosEmisor emisor = mock(DatosEmisor.class);
        when(emisor.razonSocial()).thenReturn("MAGNETIXDIAN SAS");
        when(emisor.numeroIdentificacion()).thenReturn("900123456");
        when(emisor.direccion()).thenReturn("Calle 1 # 2-3");
        DatosAdquirente adquirente = mock(DatosAdquirente.class);
        when(adquirente.nombre()).thenReturn("Cliente Ejemplo");
        when(adquirente.numeroDocumento()).thenReturn("800123456");

        factura = mock(FacturaDatos.class);
        when(factura.tipo()).thenReturn(TipoDocumento.FACTURA_VENTA);
        when(factura.numeroCompleto()).thenReturn("FE 1 1 1001");
        when(factura.fechaEmision()).thenReturn(LocalDate.of(2026, 1, 15));
        when(factura.horaEmision()).thenReturn(LocalTime.of(10, 30));
        when(factura.emisor()).thenReturn(emisor);
        when(factura.adquirente()).thenReturn(adquirente);
        when(factura.lineas()).thenReturn(List.<LineaFactura>of());
        when(factura.valorSubtotal()).thenReturn(BigDecimal.valueOf(100000));
        when(factura.totalImpuestos()).thenReturn(BigDecimal.valueOf(19000));
        when(factura.totalRetenciones()).thenReturn(BigDecimal.ZERO);
        when(factura.totalPagar()).thenReturn(BigDecimal.valueOf(119000));
    }

    @Test
    void aplicaNombreYPieDeLaMarca() throws IOException {
        BrandingDto branding = new BrandingDto(1L, "mi-empresa", "Mi Empresa", null, "#0E5C55", "#D98A2B",
                "Roboto", "Gracias por su compra", null, null, "#0E5C55", "#FFFFFF");

        byte[] pdf = generador.generar(factura, "CUFE-123", "https://ejemplo.test/qr", branding);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String texto = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(texto).contains("Mi Empresa").contains("Gracias por su compra");
    }

    @Test
    void ignoraMarcaInvalidaSinFallar() {
        BrandingDto branding = new BrandingDto(1L, "mi-empresa", null, "/etc/passwd/seguro.png", "color-invalido",
                "zzz", "Fuente Inexistente", null, null, null, null, null);

        byte[] pdf = generador.generar(factura, "CUFE-123", "https://ejemplo.test/qr", branding);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void firmaOriginalSigueFuncionando() {
        byte[] pdf = generador.generar(factura, "CUFE-123", "https://ejemplo.test/qr");
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
