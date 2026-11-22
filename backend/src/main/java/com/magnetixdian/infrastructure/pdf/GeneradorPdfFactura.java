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
import com.magnetixdian.interfaces.dto.BrandingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class GeneradorPdfFactura {

    private final String directorioUpload;

    public GeneradorPdfFactura() {
        this("./uploads");
    }

    @Autowired
    public GeneradorPdfFactura(@Value("${magnetixdian.upload.dir:./uploads}") String directorioUpload) {
        this.directorioUpload = directorioUpload == null || directorioUpload.isBlank() ? "./uploads" : directorioUpload;
    }

    public byte[] generar(FacturaDatos f, String cufe, String qrUrl) {
        return generar(f, cufe, qrUrl, null);
    }

    public byte[] generar(FacturaDatos f, String cufe, String qrUrl, BrandingDto branding) {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            String nombreFuente = fuente(branding);
            Font titulo = FontFactory.getFont(nombreFuente, 14);
            Font normal = FontFactory.getFont(nombreFuente, 9);
            Font bold = FontFactory.getFont(nombreFuente, Font.BOLD, 9);
            Color primario = color(branding == null ? null : branding.primaryColor());
            Color acento = color(branding == null ? null : branding.accentColor());
            if (primario != null) {
                titulo.setColor(primario);
            }

            Image logo = cargarLogo(branding);
            if (logo != null) {
                logo.scaleToFit(90, 50);
                doc.add(logo);
            }
            String nombreComercial = texto(branding == null ? null : branding.displayName());
            doc.add(new Paragraph(nombreComercial == null ? f.emisor().razonSocial() : nombreComercial, titulo));
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
            encabezado(tabla, bold, acento, "#", "Descripción", "Cant.", "V. Unit.", "IVA", "Total");
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

            String pie = texto(branding == null ? null : branding.invoiceFooter());
            if (pie != null) {
                doc.add(new Paragraph(pie, normal));
            }

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

    private Image cargarLogo(BrandingDto branding) {
        try {
            String ruta = texto(branding == null ? null : branding.logoUrl());
            if (ruta == null) {
                return null;
            }
            Path archivo = Paths.get(ruta).getFileName();
            if (archivo == null) {
                return null;
            }
            Path completo = Paths.get(directorioUpload, archivo.toString());
            if (!Files.isRegularFile(completo)) {
                return null;
            }
            return Image.getInstance(completo.toAbsolutePath().toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private String fuente(BrandingDto branding) {
        String familia = texto(branding == null ? null : branding.fontFamily());
        if (familia == null) {
            return FontFactory.HELVETICA;
        }
        String minusculas = familia.toLowerCase(java.util.Locale.ROOT);
        if (minusculas.contains("courier") || minusculas.contains("mono")) {
            return FontFactory.COURIER;
        }
        if (minusculas.contains("times") || minusculas.contains("serif")) {
            return FontFactory.TIMES;
        }
        return FontFactory.HELVETICA;
    }

    private Color color(String valor) {
        try {
            if (valor == null || !valor.matches("#?[0-9a-fA-F]{6}")) {
                return null;
            }
            String hex = valor.startsWith("#") ? valor.substring(1) : valor;
            return new Color(Integer.parseInt(hex, 16));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
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

    private void encabezado(PdfPTable t, Font font, Color color, String... cols) {
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c, font));
            if (color != null) {
                cell.setBackgroundColor(color);
                cell.setGrayFill(0f);
            } else {
                cell.setGrayFill(0.9f);
            }
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
