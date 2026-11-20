package com.magnetixdian.infrastructure.transmision.dian;

import com.magnetixdian.domain.transmision.CanalTransmision;
import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.apache.xml.security.Init;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;

@Component
public class ClienteDianSoap implements CanalTransmision {

    private static final String NS_SOAP = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String NS_WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String NS_WSU = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final String NS_X509 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3";
    private static final String SOAP_ACTION = "http://wcf.dian.colombia/IWcfDianCustomerServices/SendBillSync";

    private final String urlHabilitacion;
    private final String urlProduccion;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    public ClienteDianSoap(
            @Value("${magnetixdian.dian.url-habilitacion:https://vpfe-hab.dian.gov.co/WcfDianCustomerServices.svc}") String urlHabilitacion,
            @Value("${magnetixdian.dian.url-produccion:https://vpfe.dian.gov.co/WcfDianCustomerServices.svc}") String urlProduccion,
            @Value("${magnetixdian.dian.connect-timeout-ms:30000}") long connectTimeoutMs,
            @Value("${magnetixdian.dian.request-timeout-ms:60000}") long requestTimeoutMs) {
        this.urlHabilitacion = urlHabilitacion;
        this.urlProduccion = urlProduccion;
        this.connectTimeout = Duration.ofMillis(connectTimeoutMs);
        this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
        if (!Init.isInitialized()) {
            Init.init();
        }
    }

    @Override
    public String nombre() {
        return "DIAN";
    }

    @Override
    public ResultadoTransmision enviar(String xmlFirmado, String cufe, DatosEnvio datos, EmpresaFeConfigJpa config) {
        if (config.getCertificadoPath() == null || config.getCertificadoPath().isBlank()
                || config.getCertificadoPassword() == null || config.getCertificadoPassword().isBlank()) {
            return ResultadoTransmision.error("Canal DIAN sin certificado configurado", null);
        }
        String endpoint = datos.ambiente() == 1 ? urlProduccion : urlHabilitacion;
        try {
            URI uri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return ResultadoTransmision.error("Endpoint DIAN inválido", null);
            }
            String fileName = (datos.prefijo() == null ? "" : datos.prefijo()) + datos.numero();
            String envelope = construirEnvelope(fileName, xmlFirmado, config);

            HttpClient client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(requestTimeout)
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", SOAP_ACTION)
                    .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ResultadoTransmision.error("La DIAN respondió con estado " + response.statusCode(), body);
            }

            RespuestaSoap resultado = parsear(body);
            if (resultado.fault()) {
                return ResultadoTransmision.error(resultado.mensaje() != null ? resultado.mensaje() : "La DIAN devolvió un fault", body);
            }
            if (resultado.aceptado()) {
                return ResultadoTransmision.ok(cufe, resultado.mensaje() != null ? resultado.mensaje() : "Documento enviado a la DIAN", body);
            }
            return ResultadoTransmision.error(resultado.mensaje() != null ? resultado.mensaje() : "La DIAN rechazó el documento", body);
        } catch (Exception e) {
            return ResultadoTransmision.error("Error comunicándose con la DIAN: " + e.getMessage(), null);
        }
    }

    private String construirEnvelope(String fileName, String xmlFirmado, EmpresaFeConfigJpa config) throws Exception {
        DocumentBuilderFactory factory = documentFactory();
        Document documento = factory.newDocumentBuilder().newDocument();

        Element envelope = documento.createElementNS(NS_SOAP, "soap:Envelope");
        documento.appendChild(envelope);
        Element header = documento.createElementNS(NS_SOAP, "soap:Header");
        envelope.appendChild(header);
        Element body = documento.createElementNS(NS_SOAP, "soap:Body");
        body.setAttribute("Id", "cuerpo");
        envelope.appendChild(body);

        Element seguridad = documento.createElementNS(NS_WSSE, "wsse:Security");
        seguridad.setAttributeNS(NS_SOAP, "soap:mustUnderstand", "1");
        header.appendChild(seguridad);

        Element timestamp = documento.createElementNS(NS_WSU, "wsu:Timestamp");
        OffsetDateTime ahora = OffsetDateTime.now();
        String formato = DateTimeFormatter.ISO_INSTANT.format(ahora.toInstant());
        appendTexto(documento, timestamp, "wsu:Created", NS_WSU, formato);
        appendTexto(documento, timestamp, "wsu:Expires", NS_WSU,
                DateTimeFormatter.ISO_INSTANT.format(ahora.plusMinutes(5).toInstant()));
        seguridad.appendChild(timestamp);

        Element envio = documento.createElementNS("http://wcf.dian.colombia", "wcf:SendBillSync");
        appendTexto(documento, envio, "fileName", null, fileName);
        appendTexto(documento, envio, "contentFile", null,
                Base64.getEncoder().encodeToString(xmlFirmado.getBytes(StandardCharsets.UTF_8)));
        body.appendChild(envio);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] password = config.getCertificadoPassword().toCharArray();
        try (InputStream entrada = Files.newInputStream(Path.of(config.getCertificadoPath()))) {
            keyStore.load(entrada, password);
        }
        String alias = null;
        for (String candidato : Collections.list(keyStore.aliases())) {
            if (keyStore.isKeyEntry(candidato)) {
                alias = candidato;
                break;
            }
        }
        if (alias == null) {
            throw new IllegalStateException("El certificado no tiene clave privada");
        }
        PrivateKey clavePrivada = (PrivateKey) keyStore.getKey(alias, password);
        X509Certificate certificado = (X509Certificate) keyStore.getCertificate(alias);
        if (clavePrivada == null || certificado == null) {
            throw new IllegalStateException("No se pudo cargar la clave privada del certificado");
        }

        Element token = documento.createElementNS(NS_WSSE, "wsse:BinarySecurityToken");
        token.setAttribute("ValueType", NS_X509);
        token.setAttributeNS(NS_WSU, "wsu:Id", "token-certificado");
        token.setTextContent(Base64.getEncoder().encodeToString(certificado.getEncoded()));
        seguridad.appendChild(token);

        XMLSignature firma = new XMLSignature(documento, null,
                XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256,
                Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        Transforms transformaciones = new Transforms(documento);
        transformaciones.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transformaciones.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        firma.addDocument("#cuerpo", transformaciones,
                MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256, null, null);
        firma.addKeyInfo(certificado);
        seguridad.appendChild(firma.getElement());
        firma.sign(clavePrivada);

        TransformerFactory transformadorFactory = TransformerFactory.newInstance();
        transformadorFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformador = transformadorFactory.newTransformer();
        transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformador.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        StringWriter salida = new StringWriter();
        transformador.transform(new DOMSource(documento), new StreamResult(salida));
        return salida.toString();
    }

    private RespuestaSoap parsear(String body) {
        if (body == null || body.isBlank()) {
            return new RespuestaSoap(false, true, "Respuesta vacía de la DIAN");
        }
        try {
            Document documento = documentFactory().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            boolean fault = documento.getElementsByTagNameNS("*", "Fault").getLength() > 0;
            String accepted = texto(documento, "IsAccepted");
            String statusCode = texto(documento, "StatusCode");
            String statusMessage = texto(documento, "StatusMessage");
            boolean aceptado = !fault
                    && (Boolean.parseBoolean(accepted) || "1".equals(accepted) || "100".equals(statusCode));
            return new RespuestaSoap(aceptado, fault, statusMessage);
        } catch (Exception e) {
            return new RespuestaSoap(false, false, "No se pudo interpretar la respuesta de la DIAN");
        }
    }

    private DocumentBuilderFactory documentFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private void appendTexto(Document documento, Element padre, String nombre, String namespace, String valor) {
        Element elemento = namespace == null
                ? documento.createElement(nombre)
                : documento.createElementNS(namespace, nombre);
        elemento.appendChild(documento.createTextNode(valor == null ? "" : valor));
        padre.appendChild(elemento);
    }

    private String texto(Document documento, String etiqueta) {
        NodeList nodos = documento.getElementsByTagNameNS("*", etiqueta);
        if (nodos.getLength() == 0) {
            return null;
        }
        String valor = nodos.item(0).getTextContent();
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private record RespuestaSoap(boolean aceptado, boolean fault, String mensaje) {
    }
}
