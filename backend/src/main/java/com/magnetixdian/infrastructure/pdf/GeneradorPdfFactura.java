package com.magnetixdian.infrastructure.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.factura.LineaFactura;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Genera la representación gráfica (PDF) de la factura electrónica con el
 * código QR de validación DIAN.
 */
@Component
public class GeneradorPdfFactura {

    public byte[] generar(FacturaDatos f, String cufe, String qrUrl) {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            doc.add(new Paragraph(f.emisor().razonSocial(), titulo));
            doc.add(new Paragraph("NIT " + f.emisor().numeroIdentificacion()
                    + (f.emisor().direccion() != null ? " · " + f.emisor().direccion() : ""), normal));
            doc.add(new Paragraph(f.tipo().getProfileId(), normal));
            doc.add(new Paragraph(" ", normal));

            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.addCell(celda("Documento: " + f.numeroCompleto(), bold));
            info.addCell(celda("Fecha: " + f.fechaEmision() + " " + f.horaEmision(), normal));
            info.addCell(celda("Adquirente: " + f.adquirente().nombre(), normal));
            info.addCell(celda("NIT/CC: " + f.adquirente().numeroDocumento(), normal));
            info.addCell(celda("CUFE/CUDE:", bold));
            info.addCell(celda(cufe, normal));
            doc.add(info);
            doc.add(new Paragraph(" ", normal));

            PdfPTable tabla = new PdfPTable(new float[]{1, 4, 1.2f, 1.6f, 1.4f, 1.6f});
            tabla.setWidthPercentage(100);
            encabezado(tabla, bold, "#", "Descripción", "Cant.", "V. Unit.", "IVA", "Total");
            int n = 1;
            for (LineaFactura l : f.lineas()) {
                tabla.addCell(celda(String.valueOf(n++), normal));
                tabla.addCell(celda(l.descripcion(), normal));
                tabla.addCell(celda(cantidad(l.cantidad()), normal));
                tabla.addCell(celda(money(l.valorUnitario()), normal));
                tabla.addCell(celda(money(l.ivaValor()), normal));
                tabla.addCell(celda(money(l.totalLinea()), normal));
            }
            doc.add(tabla);
            doc.add(new Paragraph(" ", normal));

            PdfPTable totales = new PdfPTable(new float[]{3, 1.4f});
            totales.setWidthPercentage(45);
            totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
            fila(totales, bold, "Subtotal", money(f.valorSubtotal()));
            fila(totales, bold, "Impuestos", money(f.totalImpuestos()));
            fila(totales, bold, "Retenciones", money(f.totalRetenciones()));
            fila(totales, bold, "Total a pagar", money(f.totalPagar()));
            doc.add(totales);

            doc.add(new Paragraph(" ", normal));
            byte[] qr = qrPng(qrUrl, 120);
            if (qr != null) {
                Image img = Image.getInstance(qr);
                img.scaleToFit(110, 110);
                doc.add(img);
            }
            doc.add(new Paragraph("URL validación: " + qrUrl, normal));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la factura", e);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
    }

    private byte[] qrPng(String contenido, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, size, size);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void encabezado(PdfPTable t, Font font, String... cols) {
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c, font));
            cell.setGrayFill(0.9f);
            t.addCell(cell);
        }
    }

    private void fila(PdfPTable t, Font font, String label, String valor) {
        t.addCell(celda(label, font));
        PdfPCell v = celda(valor, font);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(v);
    }

    private PdfPCell celda(String texto, Font font) {
        return new PdfPCell(new Phrase(texto == null ? "" : texto, font));
    }

    private String money(BigDecimal v) {
        return "$ " + (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String cantidad(BigDecimal v) {
        return (v == null ? BigDecimal.ONE : v).stripTrailingZeros().toPlainString();
    }
}
