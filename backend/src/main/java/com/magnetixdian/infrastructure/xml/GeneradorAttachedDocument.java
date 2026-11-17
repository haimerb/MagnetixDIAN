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
 * Genera el AttachedDocument (contenedor electrónico) que envuelve el documento
 * firmado para su entrega al adquirente.
 */
@Component
public class GeneradorAttachedDocument {

    private static final String NS_ATT = "urn:oasis:names:specification:ubl:schema:xsd:AttachedDocument-2";
    private static final String NS_CBC = GeneradorUblFactura.NS_UBL;
    private static final String NS_CAC = GeneradorUblFactura.NS_CAC;

    public String generar(String numeroDocumento, String fechaEmision, String horaEmision,
                          String tipoDocumento, String documentoFirmado) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().newDocument();
            Element raiz = doc.createElementNS(NS_ATT, "AttachedDocument");
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cbc", NS_CBC);
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:cac", NS_CAC);
            doc.appendChild(raiz);

            cbc(doc, raiz, "UBLVersionID", "UBL 2.1");
            cbc(doc, raiz, "CustomizationID", "Documentos adjuntos");
            cbc(doc, raiz, "ID", numeroDocumento);
            cbc(doc, raiz, "IssueDate", fechaEmision);
            cbc(doc, raiz, "IssueTime", horaEmision);
            cbc(doc, raiz, "ParentDocumentID", numeroDocumento);
            cbc(doc, raiz, "DocumentType", tipoDocumento);

            Element attachment = el(doc, NS_CAC, "cac:Attachment");
            Element extRef = el(doc, NS_CAC, "cac:ExternalReference");
            cbc(doc, extRef, "cbc:MimeCode", "text/xml");
            cbc(doc, extRef, "cbc:EncodingCode", "UTF-8");
            cbc(doc, extRef, "cbc:Description", "Documento electrónico firmado");
            attachment.appendChild(extRef);
            raiz.appendChild(attachment);

            // Documento embebido como texto
            Element desc = el(doc, NS_CAC, "cac:Attachment");
            Element ref = el(doc, NS_CAC, "cac:ExternalReference");
            cbc(doc, ref, "cbc:Description", documentoFirmado);
            desc.appendChild(ref);
            raiz.appendChild(desc);

            return serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el AttachedDocument", e);
        }
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
