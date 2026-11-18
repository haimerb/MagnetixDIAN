package com.magnetixdian.infrastructure.xml;

import com.magnetixdian.domain.factura.EventoDian;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Genera el documento {@code ApplicationResponse} (UBL 2.1) con el que se
 * registran los eventos DIAN 030–034 sobre una factura electrónica.
 */
@Component
public class GeneradorApplicationResponse {

    private static final String NS_UBL = GeneradorUblFactura.NS_UBL;
    private static final String NS_CAC = GeneradorUblFactura.NS_CAC;
    private static final String NS_EXT = GeneradorUblFactura.NS_EXT;
    private static final String NS_STS = GeneradorUblFactura.NS_STS;
    private static final String NS_AR = "urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss-05:00");

    public String generar(FacturaJpa f, EventoDian evento, String cufe, String idEvento,
                          String softwareId, String softwareSecurityCode, String nitAutorizado) {
        try {
            Document doc = newDocument();
            Element raiz = el(doc, NS_AR, "ApplicationResponse");
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cbc", NS_UBL);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cac", NS_CAC);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ext", NS_EXT);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:sts", NS_STS);
            doc.appendChild(raiz);

            raiz.appendChild(extensiones(doc, f, softwareId, softwareSecurityCode, nitAutorizado));

            cbc(doc, raiz, "UBLVersionID", "UBL 2.1");
            cbc(doc, raiz, "CustomizationID", "urn:oasis:names:specification:ubl:xpath:ApplicationResponse-2.0:sbs-1.0-draft");
            cbc(doc, raiz, "ProfileID", "DIAN 2.1: ApplicationResponse de Factura Electrónica de Venta");
            cbc(doc, raiz, "ID", idEvento);
            OffsetDateTime ahora = OffsetDateTime.now();
            cbc(doc, raiz, "IssueDate", ahora.format(FECHA));
            cbc(doc, raiz, "IssueTime", ahora.format(HORA));

            EmpresaJpa e = f.getEmpresa();
            raiz.appendChild(party(doc, "cac:SenderParty", e.getNit(), "31", e.getRazonSocial()));
            raiz.appendChild(party(doc, "cac:ReceiverParty", f.getAdquirenteNumDoc(),
                    f.getAdquirenteTipoDoc(), f.getAdquirenteNombre()));

            Element resp = el(doc, NS_CAC, "cac:DocumentResponse");
            Element r = el(doc, NS_CAC, "cac:Response");
            cbc(doc, r, "cbc:ResponseCode", evento.getCodigo());
            cbc(doc, r, "cbc:Description", evento.getDescripcion());
            resp.appendChild(r);
            Element ref = el(doc, NS_CAC, "cac:DocumentReference");
            cbc(doc, ref, "cbc:ID", f.getNumeroCompleto());
            Element uuid = cbc(doc, ref, "cbc:UUID", cufe == null ? "" : cufe);
            uuid.setAttribute("schemeName", "CUFE-SHA384");
            resp.appendChild(ref);
            raiz.appendChild(resp);

            return serializar(doc);
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException("No se pudo inicializar el generador de eventos", ex);
        }
    }

    private Element extensiones(Document doc, FacturaJpa f, String softwareId,
                                String softwareSecurityCode, String nitAutorizado) {
        Element ext = el(doc, NS_EXT, "ext:UBLExtensions");
        Element ublext = el(doc, NS_EXT, "ext:UBLExtension");
        Element content = el(doc, NS_EXT, "ext:ExtensionContent");
        Element dian = el(doc, NS_STS, "sts:DianExtensions");
        Element provider = el(doc, NS_STS, "sts:SoftwareProvider");
        Element providerId = sts(doc, provider, "sts:ProviderID", f.getEmpresa().getNit());
        providerId.setAttribute("schemeName", "31");
        sts(doc, provider, "sts:SoftwareID", softwareId == null ? "" : softwareId);
        dian.appendChild(provider);
        sts(doc, dian, "sts:SoftwareSecurityCode", softwareSecurityCode == null ? "" : softwareSecurityCode);
        Element authProvider = el(doc, NS_STS, "sts:AuthorizationProvider");
        Element authId = sts(doc, authProvider, "sts:AuthorizationProviderID",
                nitAutorizado == null ? "" : nitAutorizado);
        authId.setAttribute("schemeName", "31");
        authId.setAttribute("schemeID", "4");
        dian.appendChild(authProvider);
        content.appendChild(dian);
        ublext.appendChild(content);
        ext.appendChild(ublext);
        return ext;
    }

    private Element party(Document doc, String qname, String id, String schemeName, String nombre) {
        Element party = el(doc, NS_CAC, qname);
        Element partyId = el(doc, NS_CAC, "cac:PartyIdentification");
        Element idEl = cbc(doc, partyId, "cbc:ID", id == null ? "" : id);
        idEl.setAttribute("schemeName", schemeName == null ? "31" : schemeName);
        party.appendChild(partyId);
        Element partyName = el(doc, NS_CAC, "cac:PartyName");
        cbc(doc, partyName, "cbc:Name", nombre == null ? "" : nombre);
        party.appendChild(partyName);
        return party;
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
            throw new IllegalStateException("No se pudo serializar el ApplicationResponse", e);
        }
    }
}
