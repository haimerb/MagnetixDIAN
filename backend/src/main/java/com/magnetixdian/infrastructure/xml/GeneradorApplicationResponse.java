package com.magnetixdian.infrastructure.xml;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

/**
 * Genera el ApplicationResponse (registro de evento RADIAN): acuse de recibo,
 * reclamo, recibo del bien o aceptación.
 */
@Component
public class GeneradorApplicationResponse {

    private static final String NS_APP = "urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2";
    private static final String NS_CBC = GeneradorUblFactura.NS_UBL;
    private static final String NS_CAC = GeneradorUblFactura.NS_CAC;

    public String generar(String id, String cude, int ambiente, String fecha, String hora,
                          String tipoEvento, String nitEmisor, String nitAdquirente,
                          String numeroFactura, String cufeFactura, String descripcion) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().newDocument();
            Element raiz = doc.createElementNS(NS_APP, "ApplicationResponse");
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cbc", NS_CBC);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cac", NS_CAC);
            doc.appendChild(raiz);

            cbc(doc, raiz, "UBLVersionID", "UBL 2.1");
            cbc(doc, raiz, "CustomizationID", "1");
            cbc(doc, raiz, "ProfileID", "DIAN 2.1: ApplicationResponse de Factura Electrónica de Venta");
            cbc(doc, raiz, "ProfileExecutionID", String.valueOf(ambiente));
            cbc(doc, raiz, "ID", id);
            Element uuid = cbc(doc, raiz, "UUID", cude);
            uuid.setAttribute("schemeName", "CUDE-SHA384");
            uuid.setAttribute("schemeID", String.valueOf(ambiente));
            cbc(doc, raiz, "IssueDate", fecha);
            cbc(doc, raiz, "IssueTime", hora);
            if (descripcion != null) {
                cbc(doc, raiz, "Note", descripcion);
            }

            Element sender = el(doc, NS_CAC, "cac:SenderParty");
            sender.appendChild(partyId(doc, nitAdquirente));
            raiz.appendChild(sender);

            Element receiver = el(doc, NS_CAC, "cac:ReceiverParty");
            receiver.appendChild(partyId(doc, nitEmisor));
            raiz.appendChild(receiver);

            Element resp = el(doc, NS_CAC, "cac:DocumentResponse");
            Element ref = el(doc, NS_CAC, "cac:DocumentReference");
            cbc(doc, ref, "cbc:ID", numeroFactura);
            Element refUuid = cbc(doc, ref, "cbc:UUID", cufeFactura);
            refUuid.setAttribute("schemeName", "CUFE-SHA384");
            resp.appendChild(ref);
            Element result = el(doc, NS_CAC, "cac:Response");
            cbc(doc, result, "cbc:ResponseCode", tipoEvento);
            cbc(doc, result, "cbc:Description", descripcion);
            resp.appendChild(result);
            raiz.appendChild(resp);

            return serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el ApplicationResponse", e);
        }
    }

    private Element partyId(Document doc, String nit) {
        Element pid = el(doc, NS_CAC, "cac:PartyIdentification");
        Element id = cbc(doc, pid, "cbc:ID", nit);
        id.setAttribute("schemeName", "31");
        return pid;
    }

    private Element el(Document doc, String ns, String qname) {
        return doc.createElementNS(ns, qname);
    }

    private Element cbc(Document doc, Element parent, String qname, String valor) {
        Element e = el(doc, NS_CBC, "cbc:" + qname);
        e.setTextContent(valor == null ? "" : valor);
        parent.appendChild(e);
        return e;
    }

    private String serializar(Document doc) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter w = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(w));
        return w.toString();
    }
}
