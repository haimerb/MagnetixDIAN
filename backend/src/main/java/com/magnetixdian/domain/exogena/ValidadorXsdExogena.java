package com.magnetixdian.domain.exogena;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;

@Component
public class ValidadorXsdExogena {

    public void validar(Integer ano, String formato, String xml) {
        String ruta = "/xsd/exogena/" + ano + "/" + formato + ".xsd";
        ClassPathResource recurso = new ClassPathResource(ruta);
        if (!recurso.exists()) {
            throw new IllegalArgumentException("XSD no disponible para exógena " + formato
                    + " año " + ano + ". Ruta esperada: " + ruta);
        }
        try {
            SchemaFactory fabricaEsquema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            fabricaEsquema.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabricaEsquema.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Schema esquema = fabricaEsquema.newSchema(
                    new StreamSource(recurso.getInputStream()));
            Validator validador = esquema.newValidator();
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilderFactory fabricaDocumento = DocumentBuilderFactory.newInstance();
            fabricaDocumento.setNamespaceAware(true);
            fabricaDocumento.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document documento = fabricaDocumento.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml == null ? "" : xml)));
            validador.validate(new DOMSource(documento));
        } catch (SAXException | ParserConfigurationException | IOException ex) {
            throw new IllegalArgumentException("XML de exógena " + formato + " año " + ano
                    + " no válido: " + ex.getMessage(), ex);
        }
    }
}
