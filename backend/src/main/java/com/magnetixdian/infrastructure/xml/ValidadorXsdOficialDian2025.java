package com.magnetixdian.infrastructure.xml;

import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

@Component
public class ValidadorXsdOficialDian2025 {

    public void validar(String xml, String formato) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("XML vacío para el formato " + formato);
        }
        String recurso = "/xsd/dian/2025/" + formato + ".xsd";
        try (InputStream xsd = getClass().getResourceAsStream(recurso)) {
            if (xsd == null) {
                throw new IllegalArgumentException("No se encontró el XSD local del formato " + formato);
            }
            SchemaFactory fabrica = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Schema schema = fabrica.newSchema(new StreamSource(xsd));
            Validator validador = schema.newValidator();
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validador.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException ex) {
            throw new IllegalArgumentException("XML del formato " + formato
                    + " no cumple el XSD local: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo validar el XSD local del formato " + formato, ex);
        }
    }
}
