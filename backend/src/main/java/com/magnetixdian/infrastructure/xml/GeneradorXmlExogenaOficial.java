package com.magnetixdian.infrastructure.xml;

import com.magnetixdian.domain.exogena.DocumentoExogena;
import com.magnetixdian.domain.exogena.RegistroExogena;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

@Component
public class GeneradorXmlExogenaOficial {

    private final ValidadorXsdOficialDian2025 validador;

    public GeneradorXmlExogenaOficial(ValidadorXsdOficialDian2025 validador) {
        this.validador = validador;
    }

    public String generar(DocumentoExogena documento) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder constructor = fabrica.newDocumentBuilder();
            Document xml = constructor.newDocument();

            Element raiz = xml.createElement("mas");
            xml.appendChild(raiz);
            Element cabecera = xml.createElement("Cab");
            raiz.appendChild(cabecera);
            agregar(cabecera, xml, "Ano", String.valueOf(documento.anio()));
            agregar(cabecera, xml, "CodCpt", String.valueOf(documento.codCpt()));
            agregar(cabecera, xml, "Formato", documento.formato());
            agregar(cabecera, xml, "Version", documento.version());
            agregar(cabecera, xml, "NumEnvio", String.valueOf(documento.numEnvio()));
            agregar(cabecera, xml, "FecEnvio", documento.fecEnvio());
            agregar(cabecera, xml, "FecInicial", documento.fecInicial());
            agregar(cabecera, xml, "FecFinal", documento.fecFinal());
            agregar(cabecera, xml, "ValorTotal", documento.valorTotal());
            agregar(cabecera, xml, "CantReg", String.valueOf(documento.cantReg()));

            for (RegistroExogena registro : documento.registros()) {
                Element detalle = xml.createElement(documento.elementoDetalle());
                raiz.appendChild(detalle);
                registro.atributos().forEach(detalle::setAttribute);
            }

            TransformerFactory fabricaTransformacion = TransformerFactory.newInstance();
            fabricaTransformacion.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformador = fabricaTransformacion.newTransformer();
            transformador.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.ISO_8859_1.name());
            transformador.setOutputProperty(OutputKeys.INDENT, "yes");
            transformador.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter salida = new StringWriter();
            transformador.transform(new DOMSource(xml), new StreamResult(salida));
            String resultado = salida.toString();
            validador.validar(resultado, documento.formato());
            return resultado;
        } catch (ParserConfigurationException | TransformerException ex) {
            throw new IllegalStateException("No se pudo generar el XML del formato " + documento.formato(), ex);
        }
    }

    private void agregar(Element padre, Document documento, String nombre, String valor) {
        Element elemento = documento.createElement(nombre);
        elemento.setTextContent(valor);
        padre.appendChild(elemento);
    }
}
