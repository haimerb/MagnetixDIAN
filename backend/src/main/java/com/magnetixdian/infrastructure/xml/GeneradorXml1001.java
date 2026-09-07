package com.magnetixdian.infrastructure.xml;

import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.OperacionJpa;
import com.magnetixdian.infrastructure.persistence.OperacionRepository;
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
import java.util.List;

/**
 * Genera el XML del medio magnético formato 1001 conforme al esquema
 * {@code medio-magnetico-1001.xsd} (namespaces DIAN, estructura cuenta +
 * detalle), listo para el prevalidador y presentación MUISCA.
 */
@Component
public class GeneradorXml1001 {

    private static final String NS = "urn:oecd:ties:ds:dian:magnetixdian";

    private final OperacionRepository operacionRepository;

    public GeneradorXml1001(OperacionRepository operacionRepository) {
        this.operacionRepository = operacionRepository;
    }

    /**
     * Genera el documento XML para el medio magnético dado.
     */
    public String generar(MedioMagneticoJpa medio, List<OperacionJpa> operaciones) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().newDocument();

            Element raiz = doc.createElementNS(NS, "em:MedioMagnetico");
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi",
                    "http://www.w3.org/2001/XMLSchema-instance");
            raiz.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:em", NS);
            raiz.setAttribute("anioGrabeable", String.valueOf(medio.getAnioGravable()));
            raiz.setAttribute("formato", medio.getFormato());
            raiz.setAttribute("version", medio.getVersionFormato());
            doc.appendChild(raiz);

            raiz.appendChild(encabezado(doc, medio));
            for (OperacionJpa op : operaciones) {
                raiz.appendChild(detalle(doc, op));
            }

            return serializar(doc);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("No se pudo inicializar el generador XML", e);
        }
    }

    private Element encabezado(Document doc, MedioMagneticoJpa medio) {
        var empresa = medio.getEmpresa();
        Element cuenta = doc.createElementNS(NS, "em:Cuenta");
        add(doc, cuenta, "TipoInformacion", "Exogena");
        Element periodo = doc.createElementNS(NS, "em:Periodo");
        add(doc, periodo, "Mes", "12");
        add(doc, periodo, "Anio", String.valueOf(medio.getAnioGravable()));
        cuenta.appendChild(periodo);
        add(doc, cuenta, "RazonSocial", empresa.getRazonSocial());
        add(doc, cuenta, "TipoDocumento", empresa.getTipoDocumento());
        add(doc, cuenta, "NumeroIdentificacion", empresa.getNit());
        add(doc, cuenta, "DigitoVerificacion", "");
        return cuenta;
    }

    private Element detalle(Document doc, OperacionJpa op) {
        Element info = doc.createElementNS(NS, "em:Informacion");
        add(doc, info, "PerReinOtros", clave(op));
        add(doc, info, "Concepto", op.getConcepto());
        addValor(doc, info, "ValorPago", op.getValorPago());
        addValor(doc, info, "RetencionRenta", op.getRetencionRenta());
        addValor(doc, info, "RetencionIVA", op.getRetencionIva());
        addValor(doc, info, "RetencionICA", op.getRetencionIca());
        addValor(doc, info, "RetencionTimbre", op.getRetencionTimbre());
        addValor(doc, info, "IVA", op.getIvaPagado());
        addValor(doc, info, "MayorValorCost", op.getValorGasto());
        addValor(doc, info, "NotaCredito", op.getValorNc());
        return info;
    }

    private String clave(OperacionJpa op) {
        StringBuilder sb = new StringBuilder();
        if (op.getDv() != null) {
            sb.append(op.getDv());
        }
        sb.append(op.getNumeroIdentificacion());
        return sb.toString();
    }

    private void addValor(Document doc, Element parent, String tag, BigDecimal valor) {
        if (valor != null) {
            add(doc, parent, tag, valor.toPlainString());
        }
    }

    private void add(Document doc, Element parent, String tag, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        Element el = doc.createElementNS(NS, "em:" + tag);
        el.setTextContent(valor);
        parent.appendChild(el);
    }

    private String serializar(Document doc) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("No se pudo serializar el XML", e);
        }
    }
}