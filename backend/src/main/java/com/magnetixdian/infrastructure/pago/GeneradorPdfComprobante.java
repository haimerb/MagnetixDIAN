package com.magnetixdian.infrastructure.pago;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.magnetixdian.application.pago.ComprobantePagoDto;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class GeneradorPdfComprobante {

    public byte[] generar(ComprobantePagoDto comprobante) {
        Document documento = new Document(PageSize.A4, 48, 48, 48, 48);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
            documento.add(new Paragraph("Comprobante de pago SaaS", titulo));
            documento.add(new Paragraph("Numero: " + comprobante.numero(), normal));
            documento.add(new Paragraph("Fecha: " + comprobante.fecha(), normal));
            documento.add(new Paragraph("NIT: " + comprobante.nit(), normal));
            documento.add(new Paragraph("Plan: " + comprobante.plan(), normal));
            documento.add(new Paragraph("Estado: " + comprobante.estado(), normal));
            documento.add(new Paragraph("Monto: " + comprobante.monto(), normal));
            documento.add(new Paragraph("Impuestos: " + comprobante.impuestos(), normal));
            documento.add(new Paragraph("Documento generado por MagnetixDIAN. No es un comprobante DIAN.", normal));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del comprobante.", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }
}
