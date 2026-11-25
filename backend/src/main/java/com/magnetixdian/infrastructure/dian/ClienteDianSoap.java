package com.magnetixdian.infrastructure.dian;

import com.magnetixdian.domain.dian.AmbienteDian;
import com.magnetixdian.domain.dian.CatalogoWsdlDian;
import com.magnetixdian.domain.dian.ErrorDian;
import com.magnetixdian.domain.dian.OperacionDian;
import com.magnetixdian.domain.dian.RespuestaDian;
import com.magnetixdian.domain.transmision.CanalTransmision;
import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.crypto.CifradorAes;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.apache.xml.security.Init;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Component("clienteDianSoapMotor")
public class ClienteDianSoap implements CanalTransmision {

    private static final String NS_SOAP = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String NS_WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String NS_WSU = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final String NS_X509 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3";
    private static final String NS_WCF = "http://wcf.dian.colombia";
    private static final Logger log = LoggerFactory.getLogger(ClienteDianSoap.class);

    private final String endpointProduccion;
    private final String endpointHabilitacion;
    private final String versionPorDefecto;
    private final String wsdlUrlOficial;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final CifradorAes cifrador;

    public ClienteDianSoap(
            @Value("${magnetixdian.dian.url-produccion:https://vpfe.dian.gov.co/WcfDianCustomerServices.svc}") String urlProduccion,
            @Value("${magnetixdian.dian.url-habilitacion:https://vpfe-hab.dian.gov.co/WcfDianCustomerServices.svc}") String urlHabilitacion,
            @Value("${magnetixdian.dian.wsdl.version:2.1}") String versionWsdl,
            @Value("${magnetixdian.dian.wsdl.url-oficial:}") String wsdlUrlOficial,
            @Value("${magnetixdian.dian.connect-timeout-ms:30000}") long connectTimeoutMs,
            @Value("${magnetixdian.dian.request-timeout-ms:60000}") long requestTimeoutMs,
            CifradorAes cifrador) {
        this.endpointProduccion = urlProduccion;
        this.endpointHabilitacion = urlHabilitacion;
        this.versionPorDefecto = versionWsdl == null || versionWsdl.isBlank()
                ? CatalogoWsdlDian.VERSION_POR_DEFECTO : versionWsdl.trim();
        this.wsdlUrlOficial = wsdlUrlOficial == null || wsdlUrlOficial.isBlank()
                ? null : wsdlUrlOficial.trim();
        if (this.wsdlUrlOficial == null) {
            log.warn("Los WSDL locales de DIAN son solo soporte de prueba; configure magnetixdian.dian.wsdl.url-oficial con el WSDL publicado por la DIAN antes de operar en produccion");
        }
        this.connectTimeout = Duration.ofMillis(connectTimeoutMs);
        this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
        this.cifrador = cifrador;
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
        if (xmlFirmado == null || xmlFirmado.isBlank()) {
            return ResultadoTransmision.error("No hay XML para transmitir a la DIAN", null);
        }
        try {
            RespuestaDian respuesta = sendBillSync(xmlFirmado.getBytes(StandardCharsets.UTF_8),
                    nombreArchivo(datos, cufe), datos.ambiente(), versionPorDefecto, config);
            if (respuesta.aprobada()) {
                return ResultadoTransmision.ok(respuesta.cufe() == null ? cufe : respuesta.cufe(),
                        mensaje(respuesta), null);
            }
            return ResultadoTransmision.error(mensaje(respuesta), null);
        } catch (RuntimeException e) {
            return ResultadoTransmision.error("No se pudo transmitir a la DIAN: " + e.getMessage(), null);
        }
    }

    public RespuestaDian sendBillSync(byte[] contenido, String nombreArchivo, int ambiente, String version,
                                      EmpresaFeConfigJpa config) {
        validarContenido(contenido);
        return invocar(OperacionDian.SEND_BILL_SYNC,
                List.of(new Parametro("fileName", nvl(nombreArchivo)),
                        new Parametro("contentFile", Base64.getEncoder().encodeToString(contenido))),
                ambiente, version, config);
    }

    public RespuestaDian sendTestSetAsync(byte[] contenidoZip, String nombreArchivo, String testSetId,
                                          int ambiente, String version, EmpresaFeConfigJpa config) {
        validarContenido(contenidoZip);
        if (testSetId == null || testSetId.isBlank()) {
            throw new IllegalArgumentException("El testSetId es obligatorio para SendTestSetAsync");
        }
        return invocar(OperacionDian.SEND_TEST_SET_ASYNC,
                List.of(new Parametro("fileName", nvl(nombreArchivo)),
                        new Parametro("contentFile", Base64.getEncoder().encodeToString(contenidoZip)),
                        new Parametro("testSetId", testSetId.trim())),
                ambiente, version, config);
    }

    public RespuestaDian getStatus(String trackId, int ambiente, String version, EmpresaFeConfigJpa config) {
        return consultar(OperacionDian.GET_STATUS, trackId, ambiente, version, config);
    }

    public RespuestaDian getStatusByTrackId(String trackId, int ambiente, String version,
                                            EmpresaFeConfigJpa config) {
        return consultar(OperacionDian.GET_STATUS_BY_TRACK_ID, trackId, ambiente, version, config);
    }

    public String versionPorDefecto() {
        return versionPorDefecto;
    }

    public static RespuestaDian parsearRespuesta(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            return RespuestaDian.error("Respuesta vacía de la DIAN");
        }
        try {
            Document documento = documentFactory().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(cuerpo.getBytes(StandardCharsets.UTF_8)));
            if (!elementos(documento, "Fault").isEmpty()) {
                String codigo = primero(documento, "faultcode");
                String mensaje = primero(documento, "faultstring");
                String faultCode = codigo == null ? "FAULT" : codigo;
                String faultMessage = mensaje == null ? "La DIAN devolvió un fault SOAP" : mensaje;
                return new RespuestaDian(primero(documento, "TrackId"), faultCode, faultMessage,
                        null, null, primero(documento, "Timestamp"),
                        List.of(new ErrorDian(faultCode, faultMessage)));
            }
            String trackId = primero(documento, "TrackId");
            String statusCode = primero(documento, "StatusCode");
            String statusMessage = primero(documento, "StatusMessage");
            String cufe = primero(documento, "Cufe");
            String xmlHash = primero(documento, "XmlHash");
            String timestamp = primero(documento, "Timestamp");
            if (statusCode == null || statusCode.isBlank()) {
                return new RespuestaDian(trackId, "ERROR", "La respuesta de la DIAN no incluyó StatusCode",
                        cufe, xmlHash, timestamp,
                        List.of(new ErrorDian("ERROR", "La respuesta de la DIAN no incluyó StatusCode")));
            }
            List<ErrorDian> errores = new ArrayList<>(errores(documento));
            if (!"00".equals(statusCode) && errores.isEmpty()) {
                errores.add(new ErrorDian(statusCode,
                        statusMessage == null ? "La DIAN rechazó la solicitud" : statusMessage));
            }
            return new RespuestaDian(trackId, statusCode, statusMessage, cufe, xmlHash, timestamp, errores);
        } catch (Exception e) {
            return RespuestaDian.error("No se pudo interpretar la respuesta de la DIAN");
        }
    }

    private RespuestaDian consultar(OperacionDian operacion, String trackId, int ambiente, String version,
                                    EmpresaFeConfigJpa config) {
        if (trackId == null || trackId.isBlank()) {
            throw new IllegalArgumentException("El trackId es obligatorio");
        }
        return invocar(operacion, List.of(new Parametro("TrackId", trackId.trim())), ambiente, version, config);
    }

    private RespuestaDian invocar(OperacionDian operacion, List<Parametro> parametros, int ambiente,
                                  String version, EmpresaFeConfigJpa config) {
        AmbienteDian.desde(ambiente);
        String versionNormalizada = CatalogoWsdlDian.validarVersion(version);
        if (wsdlUrlOficial == null) {
            CatalogoWsdlDian.validarRecurso(versionNormalizada, ambiente);
        }
        validarOperacionEnWsdl(leerWsdl(versionNormalizada, ambiente), operacion);
        String envelope;
        try {
            envelope = construirEnvelope(operacion, parametros, config);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo construir la solicitud SOAP DIAN", e);
        }
        URI uri = URI.create(endpoint(ambiente));
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("El endpoint DIAN configurado no es HTTPS");
        }
        HttpRequest solicitud = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", operacion.soapAction())
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                .build();
        try {
            HttpClient cliente = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
            HttpResponse<String> respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                String codigo = String.valueOf(respuesta.statusCode());
                String mensaje = "La DIAN respondió HTTP " + codigo;
                return new RespuestaDian(null, codigo, mensaje, null, null, null,
                        List.of(new ErrorDian(codigo, mensaje)));
            }
            return parsearRespuesta(respuesta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RespuestaDian.error("Transmisión DIAN interrumpida");
        } catch (Exception e) {
            return RespuestaDian.error("Error comunicándose con la DIAN: " + e.getMessage());
        }
    }

    private String endpoint(int ambiente) {
        String configurado = AmbienteDian.desde(ambiente) == AmbienteDian.PRODUCCION
                ? endpointProduccion : endpointHabilitacion;
        if (configurado == null || configurado.isBlank()) {
            throw new IllegalArgumentException("No hay endpoint DIAN configurado para el ambiente " + ambiente);
        }
        return configurado.trim();
    }

    private String leerWsdl(String version, int ambiente) {
        if (wsdlUrlOficial != null) {
            return leerWsdlOficial();
        }
        String ruta = CatalogoWsdlDian.ruta(version, ambiente);
        try (InputStream entrada = getClass().getClassLoader().getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IllegalStateException("WSDL DIAN no disponible: " + CatalogoWsdlDian.recurso(version, ambiente));
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el WSDL DIAN", e);
        }
    }

    private String leerWsdlOficial() {
        URI uri = URI.create(wsdlUrlOficial);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("La URL oficial del WSDL DIAN debe ser HTTPS");
        }
        try {
            HttpClient cliente = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
            HttpRequest solicitud = HttpRequest.newBuilder(uri).timeout(requestTimeout).GET().build();
            HttpResponse<String> respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                throw new IllegalStateException("La DIAN no entregó el WSDL oficial: HTTP " + respuesta.statusCode());
            }
            return respuesta.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Lectura del WSDL DIAN interrumpida", e);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el WSDL DIAN oficial", e);
        }
    }

    private void validarOperacionEnWsdl(String wsdl, OperacionDian operacion) {
        try {
            Document documento = documentFactory().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(wsdl.getBytes(StandardCharsets.UTF_8)));
            boolean existe = elementos(documento, "operation").stream()
                    .anyMatch(nodo -> operacion.nombre().equals(nodo.getAttributes().getNamedItem("name") == null
                            ? null : nodo.getAttributes().getNamedItem("name").getNodeValue()));
            if (!existe) {
                throw new IllegalStateException("El WSDL DIAN no modela la operación " + operacion.nombre());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("El WSDL DIAN no es válido", e);
        }
    }

    private String construirEnvelope(OperacionDian operacion, List<Parametro> parametros,
                                     EmpresaFeConfigJpa config) throws Exception {
        Document documento = documentFactory().newDocumentBuilder().newDocument();
        Element envelope = documento.createElementNS(NS_SOAP, "soap:Envelope");
        documento.appendChild(envelope);
        Element cabecera = documento.createElementNS(NS_SOAP, "soap:Header");
        envelope.appendChild(cabecera);
        Element cuerpo = documento.createElementNS(NS_SOAP, "soap:Body");
        cuerpo.setAttribute("Id", "cuerpo");
        envelope.appendChild(cuerpo);

        Element seguridad = documento.createElementNS(NS_WSSE, "wsse:Security");
        seguridad.setAttributeNS(NS_SOAP, "soap:mustUnderstand", "1");
        cabecera.appendChild(seguridad);
        Element marcaTiempo = documento.createElementNS(NS_WSU, "wsu:Timestamp");
        OffsetDateTime ahora = OffsetDateTime.now();
        appendTexto(documento, marcaTiempo, "wsu:Created", NS_WSU,
                DateTimeFormatter.ISO_INSTANT.format(ahora.toInstant()));
        appendTexto(documento, marcaTiempo, "wsu:Expires", NS_WSU,
                DateTimeFormatter.ISO_INSTANT.format(ahora.plusMinutes(5).toInstant()));
        seguridad.appendChild(marcaTiempo);

        Element solicitud = documento.createElementNS(NS_WCF, "wcf:" + operacion.nombre());
        for (Parametro parametro : parametros) {
            appendTexto(documento, solicitud, parametro.nombre(), null, parametro.valor());
        }
        cuerpo.appendChild(solicitud);

        Certificado certificado = cargarCertificado(config);
        Element token = documento.createElementNS(NS_WSSE, "wsse:BinarySecurityToken");
        token.setAttribute("ValueType", NS_X509);
        token.setAttributeNS(NS_WSU, "wsu:Id", "token-certificado");
        token.setTextContent(Base64.getEncoder().encodeToString(certificado.certificado().getEncoded()));
        seguridad.appendChild(token);

        XMLSignature firma = new XMLSignature(documento, null, XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256,
                Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        Transforms transformaciones = new Transforms(documento);
        transformaciones.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transformaciones.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        firma.addDocument("#cuerpo", transformaciones, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256, null, null);
        firma.addKeyInfo(certificado.certificado());
        KeyInfo informacion = firma.getKeyInfo();
        if (informacion != null && informacion.getElement() != null) {
            Element referenciaToken = documento.createElementNS(NS_WSSE, "wsse:SecurityTokenReference");
            Element referencia = documento.createElementNS(NS_WSSE, "wsse:Reference");
            referencia.setAttribute("URI", "#token-certificado");
            referencia.setAttribute("ValueType", NS_X509);
            referenciaToken.appendChild(referencia);
            informacion.getElement().appendChild(referenciaToken);
        }
        seguridad.appendChild(firma.getElement());
        firma.sign(certificado.clavePrivada());

        TransformerFactory fabrica = TransformerFactory.newInstance();
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformador = fabrica.newTransformer();
        transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformador.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        StringWriter salida = new StringWriter();
        transformador.transform(new DOMSource(documento), new StreamResult(salida));
        return salida.toString();
    }

    private Certificado cargarCertificado(EmpresaFeConfigJpa config) throws Exception {
        if (config == null || config.getCertificadoPath() == null || config.getCertificadoPath().isBlank()
                || config.getCertificadoPassword() == null || config.getCertificadoPassword().isBlank()) {
            throw new IllegalStateException("El certificado DIAN no está configurado");
        }
        String password = cifrador.descifrar(config.getCertificadoPassword());
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("El certificado DIAN no está configurado");
        }
        char[] clave = password.toCharArray();
        KeyStore almacen = KeyStore.getInstance("PKCS12");
        try (InputStream entrada = Files.newInputStream(Path.of(config.getCertificadoPath()))) {
            almacen.load(entrada, clave);
        }
        String alias = null;
        for (String candidato : Collections.list(almacen.aliases())) {
            if (almacen.isKeyEntry(candidato)) {
                alias = candidato;
                break;
            }
        }
        if (alias == null) {
            throw new IllegalStateException("El certificado DIAN no contiene clave privada");
        }
        PrivateKey clavePrivada = (PrivateKey) almacen.getKey(alias, clave);
        X509Certificate certificado = (X509Certificate) almacen.getCertificate(alias);
        if (clavePrivada == null || certificado == null) {
            throw new IllegalStateException("No se pudo cargar el certificado DIAN");
        }
        return new Certificado(clavePrivada, certificado);
    }

    private static DocumentBuilderFactory documentFactory() throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        return fabrica;
    }

    private static void appendTexto(Document documento, Element padre, String nombre, String namespace, String valor) {
        Element elemento = namespace == null
                ? documento.createElement(nombre)
                : documento.createElementNS(namespace, nombre);
        elemento.appendChild(documento.createTextNode(valor == null ? "" : valor));
        padre.appendChild(elemento);
    }

    private static String primero(Document documento, String nombre) {
        for (Element elemento : elementos(documento, nombre)) {
            String valor = elemento.getTextContent();
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return null;
    }

    private static List<Element> elementos(Document documento, String nombre) {
        List<Element> encontrados = new ArrayList<>();
        NodeList nodos = documento.getElementsByTagName("*");
        for (int i = 0; i < nodos.getLength(); i++) {
            Node nodo = nodos.item(i);
            if (nodo instanceof Element elemento && nombre.equalsIgnoreCase(localName(elemento))) {
                encontrados.add(elemento);
            }
        }
        return encontrados;
    }

    private static List<ErrorDian> errores(Document documento) {
        List<ErrorDian> errores = new ArrayList<>();
        for (Element elemento : elementos(documento, "Error")) {
            String mensaje = primeroHijo(elemento, "Message", "Mensaje", "ErrorMessage");
            String codigo = atributo(elemento, "code", "codigo", "Code");
            if (codigo == null) {
                codigo = primeroHijo(elemento, "Code", "Codigo");
            }
            errores.add(new ErrorDian(codigo == null ? "ERROR" : codigo,
                    mensaje == null ? elemento.getTextContent().trim() : mensaje));
        }
        for (Element elemento : elementos(documento, "ErrorMessage")) {
            String mensaje = elemento.getTextContent();
            if (mensaje != null && !mensaje.isBlank()) {
                errores.add(new ErrorDian("ERROR", mensaje.trim()));
            }
        }
        return errores;
    }

    private static String primeroHijo(Element padre, String... nombres) {
        for (String nombre : nombres) {
            for (Element elemento : elementos(padre, nombre)) {
                String valor = elemento.getTextContent();
                if (valor != null && !valor.isBlank()) {
                    return valor.trim();
                }
            }
        }
        return null;
    }

    private static List<Element> elementos(Element padre, String nombre) {
        List<Element> encontrados = new ArrayList<>();
        NodeList nodos = padre.getElementsByTagName("*");
        for (int i = 0; i < nodos.getLength(); i++) {
            Node nodo = nodos.item(i);
            if (nodo instanceof Element elemento && nombre.equalsIgnoreCase(localName(elemento))) {
                encontrados.add(elemento);
            }
        }
        return encontrados;
    }

    private static String atributo(Element elemento, String... nombres) {
        for (String nombre : nombres) {
            Node atributo = elemento.getAttributes().getNamedItem(nombre);
            if (atributo != null && !atributo.getNodeValue().isBlank()) {
                return atributo.getNodeValue().trim();
            }
        }
        return null;
    }

    private static String localName(Element elemento) {
        String local = elemento.getLocalName();
        if (local != null) {
            return local;
        }
        String nombre = elemento.getNodeName();
        int separador = nombre.indexOf(':');
        return separador < 0 ? nombre : nombre.substring(separador + 1);
    }

    private static void validarContenido(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El contenido XML enviado a la DIAN está vacío");
        }
    }

    private static String nvl(String valor) {
        return valor == null ? "" : valor;
    }

    private static String mensaje(RespuestaDian respuesta) {
        if (respuesta.statusMessage() != null && !respuesta.statusMessage().isBlank()) {
            return respuesta.statusMessage();
        }
        if (!respuesta.errores().isEmpty()) {
            return respuesta.errores().get(0).mensaje();
        }
        return "La DIAN rechazó el documento";
    }

    private static String nombreArchivo(DatosEnvio datos, String cufe) {
        String prefijo = datos == null || datos.prefijo() == null ? "" : datos.prefijo();
        long numero = datos == null ? 0L : datos.numero();
        String base = prefijo + numero;
        return base.isBlank() || "0".equals(base) ? nvl(cufe) + ".xml" : base + ".xml";
    }

    private record Parametro(String nombre, String valor) {
    }

    private record Certificado(PrivateKey clavePrivada, X509Certificate certificado) {
    }
}
