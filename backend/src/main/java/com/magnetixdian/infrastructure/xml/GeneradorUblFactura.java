package com.magnetixdian.infrastructure.xml;

import com.magnetixdian.domain.factura.DatosAdquirente;
import com.magnetixdian.domain.factura.DatosEmisor;
import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.factura.ImpuestoFactura;
import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.factura.TipoDocumento;
import com.magnetixdian.interfaces.dto.NominaElectronicaDto;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Genera documentos UBL 2.1 (Invoice / CreditNote / DebitNote) conforme al
 * Anexo Técnico de Factura Electrónica de Venta v1.9 de la DIAN, incluyendo el
 * bloque {@code sts:DianExtensions}.
 */
@Component
public class GeneradorUblFactura {

    public static final String NS_UBL = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    public static final String NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    public static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    public static final String NS_STS = "dian:gov:co:facturaelectronica:Structures-2-1";
    public static final String NS_DS = "http://www.w3.org/2000/09/xmldsig#";
    public static final String NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    public static final String NS_CREDIT = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";
    public static final String NS_DEBIT = "urn:oasis:names:specification:ubl:schema:xsd:DebitNote-2";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss-05:00");

    /**
     * Construye el XML UBL del documento (sin firma).
     */
    public String generar(FacturaDatos f, String cufe) {
        String root = f.tipo().getUblRoot();
        return generarConEncabezado(f, cufe, root, f.tipo().getCodigo(), f.tipo().getProfileId(),
                tipoCodeTag(root));
    }

    public byte[] construirDocumentoPrueba(String tipo, String testSetId, int secuencia) {
        TipoDocumento tipoDocumento = tipoPrueba(tipo);
        long numero = secuencia <= 0 ? 1 : secuencia;
        String prefijo = "TST";
        String numeroCompleto = prefijo + tipoDocumento.getCodigo() + String.format(Locale.ROOT, "%03d", numero);
        String semilla = tipoDocumento.getCodigo() + "|" + nvl(testSetId) + "|" + numeroCompleto;
        BigDecimal subtotal = new BigDecimal("1000.00");
        BigDecimal iva = new BigDecimal("190.00");
        LineaFactura linea = LineaFactura.of(1, "Documento de prueba " + tipoDocumento.name(), BigDecimal.ONE,
                subtotal, BigDecimal.valueOf(19), BigDecimal.ZERO, "94", null);
        FacturaDatos datos = new FacturaDatos(tipoDocumento, numeroCompleto, prefijo, numero,
                java.time.LocalDate.now(), java.time.LocalTime.now().withNano(0), null, 2, "COP", "1", "10", "10",
                new DatosEmisor("31", "900123456", 4, "MAGNETIXDIAN SAS", "ORDINARIO", "R-99-PN",
                        "Calle 1", "Bogota", "Bogota", "facturacion@magnetixdian.com", false),
                new DatosAdquirente("31", "800197268", null, "DIAN", "facturacion@magnetixdian.com",
                        "Cra 7", "Bogota", "ORDINARIO", "R-99-PN"),
                List.of(linea), List.of(new ImpuestoFactura("01", "IVA", subtotal, BigDecimal.valueOf(19), iva)),
                subtotal, iva, BigDecimal.ZERO, subtotal.add(iva), subtotal.add(iva),
                "Documento de prueba - testSetId " + nvl(testSetId), null, null,
                "TEST-SOFTWARE", "TEST-PIN", hashHex("SECURITY|" + semilla), nvl(testSetId),
                "RES-TEST", java.time.LocalDate.now(), java.time.LocalDate.now().minusMonths(1),
                java.time.LocalDate.now().plusYears(1), numero, numero);
        String cufe = hashHex("CUFE|" + semilla);
        return generar(datos, cufe).getBytes(StandardCharsets.UTF_8);
    }

    public String generarNomina(FacturaDatos f, NominaElectronicaDto nomina, String cufe) {
        if (nomina == null) {
            throw new IllegalArgumentException("La nómina electrónica es obligatoria.");
        }
        List<LineaFactura> lineas = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int n = 1;
        if (nomina.devengados() != null) {
            for (NominaElectronicaDto.ConceptoDto concepto : nomina.devengados()) {
                BigDecimal valor = concepto.valor() == null ? BigDecimal.ZERO : concepto.valor();
                lineas.add(new LineaFactura(n++, concepto.codigo(), concepto.descripcion(), BigDecimal.ONE,
                        "94", valor, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, valor, null));
                subtotal = subtotal.add(valor);
            }
        }
        BigDecimal deducciones = BigDecimal.ZERO;
        if (nomina.deducciones() != null) {
            for (NominaElectronicaDto.ConceptoDto concepto : nomina.deducciones()) {
                deducciones = deducciones.add(concepto.valor() == null ? BigDecimal.ZERO : concepto.valor());
            }
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        deducciones = deducciones.setScale(2, RoundingMode.HALF_UP);
        List<ImpuestoFactura> impuestos = new ArrayList<>();
        if (deducciones.signum() > 0) {
            impuestos.add(new ImpuestoFactura("05", "Deducciones de nómina", subtotal, BigDecimal.ZERO,
                    deducciones));
        }
        String observaciones = nvl(f.observaciones());
        if (!observaciones.isEmpty()) {
            observaciones += " | ";
        }
        observaciones += "Trabajador " + nvl(nomina.trabajadorNumeroDocumento()) + " - "
                + nvl(nomina.trabajadorNombre()) + " | Periodo " + nvl(nomina.periodo());
        FacturaDatos datosNomina = new FacturaDatos(f.tipo(), f.numeroCompleto(), f.prefijo(), f.numero(),
                f.fechaEmision(), f.horaEmision(), nomina.fechaPago(), f.ambiente(), f.moneda(), f.formaPago(),
                f.medioPago(), f.tipoOperacion(), f.emisor(), f.adquirente(), lineas, impuestos, subtotal,
                BigDecimal.ZERO, deducciones, subtotal, subtotal.subtract(deducciones), observaciones,
                f.cufeReferencia(), f.numeroReferencia(), f.softwareId(), f.softwarePin(),
                f.softwareSecurityCode(), f.testSetId(), f.resolucionNumero(), f.resolucionFecha(),
                f.rangoVigenciaDesde(), f.rangoVigenciaHasta(), f.rangoDesde(), f.rangoHasta());
        return generarConEncabezado(datosNomina, cufe, "Invoice", "31",
                "DIAN 2.1: Nomina Electronica", "cbc:InvoiceTypeCode");
    }

    private String generarConEncabezado(FacturaDatos f, String cufe, String root, String codigo,
                                        String profileId, String tipoCode) {
        try {
            String rootNs = switch (root) {
                case "CreditNote" -> NS_CREDIT;
                case "DebitNote" -> NS_DEBIT;
                default -> NS_INVOICE;
            };
            Document doc = newDocument();
            Element raiz = doc.createElementNS(rootNs, root);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cbc", NS_UBL);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cac", NS_CAC);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ext", NS_EXT);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:sts", NS_STS);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ds", NS_DS);
            doc.appendChild(raiz);

            raiz.appendChild(extensiones(doc, f));

            cbc(doc, raiz, "UBLVersionID", "UBL 2.1");
            cbc(doc, raiz, "CustomizationID", f.tipoOperacion());
            cbc(doc, raiz, "ProfileID", profileId);
            cbc(doc, raiz, "ProfileExecutionID", String.valueOf(f.ambiente()));
            cbc(doc, raiz, "ID", f.numeroCompleto());
            Element uuid = cbc(doc, raiz, "UUID", cufe);
            uuid.setAttribute("schemeName", f.tipo().esNota() ? "CUDE-SHA384" : "CUFE-SHA384");
            uuid.setAttribute("schemeID", String.valueOf(f.ambiente()));
            cbc(doc, raiz, "IssueDate", f.fechaEmision().format(FECHA));
            cbc(doc, raiz, "IssueTime", f.horaEmision().format(HORA));
            cbc(doc, raiz, tipoCode, codigo);
            if (f.observaciones() != null && !f.observaciones().isBlank()) {
                cbc(doc, raiz, "Note", f.observaciones());
            }
            cbc(doc, raiz, "DocumentCurrencyCode", f.moneda());
            cbc(doc, raiz, "LineCountNumeric", String.valueOf(f.lineas().size()));

            if (f.tipo().esNota() && f.cufeReferencia() != null) {
                raiz.appendChild(billingReference(doc, f));
            }

            raiz.appendChild(proveedor(doc, f.emisor()));
            raiz.appendChild(adquirente(doc, f.adquirente()));
            raiz.appendChild(paymentMeans(doc, f));
            raiz.appendChild(paymentTerms(doc, f));

            for (String tributo : new String[]{"01", "04"}) {
                BigDecimal valor = sumar(f.impuestos(), tributo);
                if (valor.signum() > 0) {
                    raiz.appendChild(taxTotal(doc, f, tributo, valor));
                }
            }
            BigDecimal ret = sumar(f.impuestos(), "05")
                    .add(sumar(f.impuestos(), "06"))
                    .add(sumar(f.impuestos(), "07"));
            if (ret.signum() > 0) {
                raiz.appendChild(withholdingTaxTotal(doc, f, ret));
            }

            raiz.appendChild(legalMonetaryTotal(doc, f));

            int n = 1;
            for (LineaFactura l : f.lineas()) {
                raiz.appendChild(linea(doc, f, l, n++));
            }
            return serializar(doc);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("No se pudo inicializar el generador UBL", e);
        }
    }

    private TipoDocumento tipoPrueba(String tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de documento de prueba es obligatorio.");
        }
        String normalizado = tipo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return switch (normalizado) {
            case "FACTURA", "FACTURAVENTA", "01" -> TipoDocumento.FACTURA_VENTA;
            case "NOTADEBITO", "DEBITNOTE", "92" -> TipoDocumento.NOTA_DEBITO;
            case "NOTACREDITO", "CREDITNOTE", "91" -> TipoDocumento.NOTA_CREDITO;
            default -> throw new IllegalArgumentException("Tipo de documento de prueba no soportado: " + tipo);
        };
    }

    private String hashHex(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular el identificador de prueba", e);
        }
    }

    private Element extensiones(Document doc, FacturaDatos f) {
        Element ext = el(doc, NS_EXT, "ext:UBLExtensions");
        Element ublext = el(doc, NS_EXT, "ext:UBLExtension");
        Element content = el(doc, NS_EXT, "ext:ExtensionContent");
        Element dian = el(doc, NS_STS, "sts:DianExtensions");

        // InvoiceControl
        Element control = el(doc, NS_STS, "sts:InvoiceControl");
        sts(doc, control, "sts:InvoiceAuthorization", nvl(f.resolucionNumero()));
        Element period = el(doc, NS_STS, "sts:AuthorizationPeriod");
        if (f.rangoVigenciaDesde() != null) {
            cbc(doc, period, "cbc:StartDate", f.rangoVigenciaDesde().format(FECHA));
        }
        if (f.rangoVigenciaHasta() != null) {
            cbc(doc, period, "cbc:EndDate", f.rangoVigenciaHasta().format(FECHA));
        }
        control.appendChild(period);
        Element autorizadas = el(doc, NS_STS, "sts:AuthorizedInvoices");
        sts(doc, autorizadas, "sts:Prefix", nvl(f.prefijo()));
        sts(doc, autorizadas, "sts:From", String.valueOf(f.rangoDesde()));
        sts(doc, autorizadas, "sts:To", String.valueOf(f.rangoHasta()));
        control.appendChild(autorizadas);
        dian.appendChild(control);

        // InvoiceSource
        Element source = el(doc, NS_STS, "sts:InvoiceSource");
        Element idCode = cbc(doc, source, "cbc:IdentificationCode", "CO");
        idCode.setAttribute("listAgencyID", "6");
        idCode.setAttribute("listAgencyName", "United Nations Economic Commission for Europe");
        idCode.setAttribute("listSchemeURI", "urn:oasis:names:specification:ubl:codelist:gc:CountryIdentificationCode-2.1");
        dian.appendChild(source);

        // SoftwareProvider
        Element provider = el(doc, NS_STS, "sts:SoftwareProvider");
        Element providerId = sts(doc, provider, "sts:ProviderID", f.nitEmisor());
        providerId.setAttribute("schemeName", "31");
        sts(doc, provider, "sts:SoftwareID", nvl(f.softwareId()));
        dian.appendChild(provider);

        // SoftwareSecurityCode
        sts(doc, dian, "sts:SoftwareSecurityCode", nvl(f.softwareSecurityCode()));
        sts(doc, dian, "sts:TestSetId", nvl(f.testSetId()));

        // AuthorizationProvider
        Element authProvider = el(doc, NS_STS, "sts:AuthorizationProvider");
        Element authId = sts(doc, authProvider, "sts:AuthorizationProviderID", "800197268");
        authId.setAttribute("schemeName", "31");
        authId.setAttribute("schemeID", "4");
        dian.appendChild(authProvider);

        content.appendChild(dian);
        ublext.appendChild(content);
        ext.appendChild(ublext);
        return ext;
    }

    private Element billingReference(Document doc, FacturaDatos f) {
        Element ref = el(doc, NS_CAC, "cac:BillingReference");
        Element inv = el(doc, NS_CAC, "cac:InvoiceDocumentReference");
        cbc(doc, inv, "cbc:ID", nvl(f.numeroReferencia()));
        Element uuid = cbc(doc, inv, "cbc:UUID", nvl(f.cufeReferencia()));
        uuid.setAttribute("schemeName", "CUFE-SHA384");
        inv.appendChild(uuid);
        ref.appendChild(inv);
        return ref;
    }

    private Element proveedor(Document doc, DatosEmisor e) {
        Element party = party(doc, e.tipoDocumento(), e.numeroIdentificacion(), e.digitoVerificacion(),
                e.razonSocial(), e.direccion(), e.municipio(), e.email(),
                e.responsabilidadFiscal(), e.regimen());
        Element wrapper = el(doc, NS_CAC, "cac:AccountingSupplierParty");
        wrapper.appendChild(party);
        return wrapper;
    }

    private Element adquirente(Document doc, DatosAdquirente a) {
        Element party = party(doc, a.tipoDocumento(), a.numeroDocumento(), a.digitoVerificacion(),
                a.nombre(), a.direccion(), a.municipio(), a.email(),
                a.responsabilidadFiscal(), a.regimen());
        Element wrapper = el(doc, NS_CAC, "cac:AccountingCustomerParty");
        wrapper.appendChild(party);
        return wrapper;
    }

    private Element party(Document doc, String tipoDoc, String numDoc, Integer dv, String nombre,
                          String direccion, String municipio, String email,
                          String responsabilidad, String regimen) {
        Element party = el(doc, NS_CAC, "cac:Party");
        Element partyId = el(doc, NS_CAC, "cac:PartyIdentification");
        Element id = cbc(doc, partyId, "cbc:ID", numDoc);
        id.setAttribute("schemeName", tipoDoc == null ? "13" : tipoDoc);
        party.appendChild(partyId);

        Element partyName = el(doc, NS_CAC, "cac:PartyName");
        cbc(doc, partyName, "cbc:Name", nombre);
        party.appendChild(partyName);

        Element address = el(doc, NS_CAC, "cac:PostalAddress");
        cbc(doc, address, "cbc:CityName", nvl(municipio));
        cbc(doc, address, "cbc:CountrySubentity", "");
        cbc(doc, address, "cbc:Line", nvl(direccion));
        Element country = el(doc, NS_CAC, "cac:Country");
        Element countryCode = cbc(doc, country, "cbc:IdentificationCode", "CO");
        countryCode.setAttribute("listAgencyID", "6");
        countryCode.setAttribute("listID", "ISO3166-1:alpha2");
        country.appendChild(countryCode);
        address.appendChild(country);
        party.appendChild(address);

        Element taxScheme = el(doc, NS_CAC, "cac:PartyTaxScheme");
        cbc(doc, taxScheme, "cbc:RegistrationName", nombre);
        Element companyId = cbc(doc, taxScheme, "cbc:CompanyID", numDoc);
        companyId.setAttribute("schemeName", tipoDoc == null ? "13" : tipoDoc);
        if (dv != null) {
            companyId.setAttribute("schemeID", String.valueOf(dv));
        }
        cbc(doc, taxScheme, "cbc:TaxLevelCode", responsabilidad == null ? "R-99-PN" : responsabilidad);
        Element ts = el(doc, NS_CAC, "cac:TaxScheme");
        cbc(doc, ts, "cbc:ID", "01");
        cbc(doc, ts, "cbc:Name", "IVA");
        taxScheme.appendChild(ts);
        party.appendChild(taxScheme);

        Element legal = el(doc, NS_CAC, "cac:PartyLegalEntity");
        cbc(doc, legal, "cbc:RegistrationName", nombre);
        Element scheme = el(doc, NS_CAC, "cac:CorporateRegistrationScheme");
        cbc(doc, scheme, "cbc:ID", "");
        legal.appendChild(scheme);
        if (email != null && !email.isBlank()) {
            Element contact = el(doc, NS_CAC, "cac:Contact");
            cbc(doc, contact, "cbc:ElectronicMail", email);
            legal.appendChild(contact);
        }
        party.appendChild(legal);
        return party;
    }

    private Element paymentMeans(Document doc, FacturaDatos f) {
        Element pm = el(doc, NS_CAC, "cac:PaymentMeans");
        cbc(doc, pm, "cbc:ID", f.medioPago());
        cbc(doc, pm, "cbc:PaymentMeansCode", f.medioPago());
        if (f.fechaVencimiento() != null) {
            cbc(doc, pm, "cbc:PaymentDueDate", f.fechaVencimiento().format(FECHA));
        }
        return pm;
    }

    private Element paymentTerms(Document doc, FacturaDatos f) {
        Element pt = el(doc, NS_CAC, "cac:PaymentTerms");
        cbc(doc, pt, "cbc:ID", f.formaPago());
        cbc(doc, pt, "cbc:PaymentMeansID", f.formaPago());
        return pt;
    }

    private Element taxTotal(Document doc, FacturaDatos f, String codigo, BigDecimal valor) {
        Element total = el(doc, NS_CAC, "cac:TaxTotal");
        cbc(doc, total, "cbc:TaxAmount", money(valor)).setAttribute("currencyID", f.moneda());
        Element sub = el(doc, NS_CAC, "cac:TaxSubtotal");
        cbc(doc, sub, "cbc:TaxableAmount", money(f.valorSubtotal())).setAttribute("currencyID", f.moneda());
        cbc(doc, sub, "cbc:TaxAmount", money(valor)).setAttribute("currencyID", f.moneda());
        Element cat = el(doc, NS_CAC, "cac:TaxCategory");
        cbc(doc, cat, "cbc:Percent", pct(f, codigo));
        Element scheme = el(doc, NS_CAC, "cac:TaxScheme");
        cbc(doc, scheme, "cbc:ID", codigo);
        cbc(doc, scheme, "cbc:Name", "01".equals(codigo) ? "IVA" : "INC");
        cat.appendChild(scheme);
        sub.appendChild(cat);
        total.appendChild(sub);
        return total;
    }

    private Element withholdingTaxTotal(Document doc, FacturaDatos f, BigDecimal valor) {
        Element total = el(doc, NS_CAC, "cac:WithholdingTaxTotal");
        cbc(doc, total, "cbc:TaxAmount", money(valor)).setAttribute("currencyID", f.moneda());
        for (String codigo : new String[]{"05", "06", "07"}) {
            BigDecimal v = sumar(f.impuestos(), codigo);
            if (v.signum() <= 0) {
                continue;
            }
            Element sub = el(doc, NS_CAC, "cac:TaxSubtotal");
            cbc(doc, sub, "cbc:TaxableAmount", money(f.valorSubtotal())).setAttribute("currencyID", f.moneda());
            cbc(doc, sub, "cbc:TaxAmount", money(v)).setAttribute("currencyID", f.moneda());
            Element cat = el(doc, NS_CAC, "cac:TaxCategory");
            cbc(doc, cat, "cbc:Percent", pct(f, codigo));
            Element scheme = el(doc, NS_CAC, "cac:TaxScheme");
            cbc(doc, scheme, "cbc:ID", codigo);
            cbc(doc, scheme, "cbc:Name", nombreRetencion(codigo));
            cat.appendChild(scheme);
            sub.appendChild(cat);
            total.appendChild(sub);
        }
        return total;
    }

    private Element legalMonetaryTotal(Document doc, FacturaDatos f) {
        Element total = el(doc, NS_CAC, "cac:LegalMonetaryTotal");
        cbc(doc, total, "cbc:LineExtensionAmount", money(f.valorSubtotal())).setAttribute("currencyID", f.moneda());
        cbc(doc, total, "cbc:TaxExclusiveAmount", money(f.valorSubtotal())).setAttribute("currencyID", f.moneda());
        cbc(doc, total, "cbc:TaxInclusiveAmount", money(f.total())).setAttribute("currencyID", f.moneda());
        cbc(doc, total, "cbc:PayableAmount", money(f.totalPagar())).setAttribute("currencyID", f.moneda());
        return total;
    }

    private Element linea(Document doc, FacturaDatos f, LineaFactura l, int n) {
        boolean nota = f.tipo().esNota();
        Element ln = el(doc, NS_CAC, nota ? "cac:CreditNoteLine" : "cac:InvoiceLine");
        cbc(doc, ln, "cbc:ID", String.valueOf(n));
        String qtyTag = nota ? "cbc:CreditedQuantity" : "cbc:InvoicedQuantity";
        Element qty = cbc(doc, ln, qtyTag, cantidad(l.cantidad()));
        qty.setAttribute("unitCode", l.unidadMedida() == null ? "94" : l.unidadMedida());
        cbc(doc, ln, "cbc:LineExtensionAmount", money(l.totalLinea())).setAttribute("currencyID", f.moneda());

        BigDecimal iva = l.ivaValor() == null ? BigDecimal.ZERO : l.ivaValor();
        if (iva.signum() > 0) {
            Element tt = el(doc, NS_CAC, "cac:TaxTotal");
            cbc(doc, tt, "cbc:TaxAmount", money(iva)).setAttribute("currencyID", f.moneda());
            Element sub = el(doc, NS_CAC, "cac:TaxSubtotal");
            cbc(doc, sub, "cbc:TaxableAmount", money(l.totalLinea())).setAttribute("currencyID", f.moneda());
            cbc(doc, sub, "cbc:TaxAmount", money(iva)).setAttribute("currencyID", f.moneda());
            Element cat = el(doc, NS_CAC, "cac:TaxCategory");
            cbc(doc, cat, "cbc:Percent", pct(l.ivaPct()));
            Element scheme = el(doc, NS_CAC, "cac:TaxScheme");
            cbc(doc, scheme, "cbc:ID", "01");
            cbc(doc, scheme, "cbc:Name", "IVA");
            cat.appendChild(scheme);
            sub.appendChild(cat);
            tt.appendChild(sub);
            ln.appendChild(tt);
        }

        Element item = el(doc, NS_CAC, "cac:Item");
        cbc(doc, item, "cbc:Description", l.descripcion());
        if (l.codigoProducto() != null) {
            Element seller = el(doc, NS_CAC, "cac:SellersItemIdentification");
            cbc(doc, seller, "cbc:ID", l.codigoProducto());
            item.appendChild(seller);
        }
        if (l.codigoUnspsc() != null && !l.codigoUnspsc().isBlank()) {
            Element std = el(doc, NS_CAC, "cac:StandardItemIdentification");
            Element id = cbc(doc, std, "cbc:ID", l.codigoUnspsc());
            id.setAttribute("schemeID", "UNSPSC");
            item.appendChild(std);
        }
        ln.appendChild(item);

        Element price = el(doc, NS_CAC, "cac:Price");
        cbc(doc, price, "cbc:PriceAmount", money(l.valorUnitario())).setAttribute("currencyID", f.moneda());
        cbc(doc, price, "cbc:BaseQuantity", cantidad(l.cantidad())).setAttribute("unitCode", l.unidadMedida() == null ? "94" : l.unidadMedida());
        ln.appendChild(price);
        return ln;
    }

    private String tipoCodeTag(String root) {
        return switch (root) {
            case "CreditNote" -> "cbc:CreditNoteTypeCode";
            case "DebitNote" -> "cbc:DebitNoteTypeCode";
            default -> "cbc:InvoiceTypeCode";
        };
    }

    private BigDecimal sumar(List<ImpuestoFactura> impuestos, String tipo) {
        if (impuestos == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal t = BigDecimal.ZERO;
        for (ImpuestoFactura i : impuestos) {
            if (tipo.equals(i.tipo()) && i.valor() != null) {
                t = t.add(i.valor());
            }
        }
        return t;
    }

    private String pct(FacturaDatos f, String tipo) {
        if (f.impuestos() != null) {
            for (ImpuestoFactura i : f.impuestos()) {
                if (tipo.equals(i.tipo()) && i.porcentaje() != null) {
                    return i.porcentaje().setScale(2, RoundingMode.HALF_UP).toPlainString();
                }
            }
        }
        return "0.00";
    }

    private String pct(BigDecimal p) {
        return (p == null ? BigDecimal.ZERO : p).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String nombreRetencion(String codigo) {
        return switch (codigo) {
            case "05" -> "Retención en la fuente";
            case "06" -> "Retención IVA";
            case "07" -> "Retención ICA";
            default -> "Retención";
        };
    }

    private String money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String cantidad(BigDecimal v) {
        return (v == null ? BigDecimal.ONE : v).stripTrailingZeros().toPlainString();
    }

    private Document newDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().newDocument();
    }

    private Element el(Document doc, String ns, String qname) {
        return doc.createElementNS(ns, qname);
    }

    private Element cbc(Document doc, Element parent, String qname, String valor) {
        Element e = el(doc, NS_UBL, qname);
        e.setTextContent(valor == null ? "" : valor);
        parent.appendChild(e);
        return e;
    }

    private Element sts(Document doc, Element parent, String qname, String valor) {
        Element e = el(doc, NS_STS, qname);
        e.setTextContent(valor == null ? "" : valor);
        parent.appendChild(e);
        return e;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String serializar(Document doc) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("No se pudo serializar el XML UBL", e);
        }
    }
}
