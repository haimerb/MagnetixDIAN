package com.magnetixdian.infrastructure.integracion.netsuite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.magnetixdian.domain.integracion.AdaptadorIntegracion;
import com.magnetixdian.domain.integracion.CapacidadIntegracion;
import com.magnetixdian.domain.integracion.ClienteIntegracion;
import com.magnetixdian.domain.integracion.DatosConexionIntegracion;
import com.magnetixdian.domain.integracion.EventoIntegracion;
import com.magnetixdian.domain.integracion.FacturaIntegracion;
import com.magnetixdian.domain.integracion.LineaFacturaIntegracion;
import com.magnetixdian.domain.integracion.ProductoIntegracion;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import com.magnetixdian.domain.integracion.ResultadoIntegracion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

@Component
public class AdaptadorNetSuite implements AdaptadorIntegracion {

    private static final String BASE_API = "/services/rest/record/v1";
    private static final Set<CapacidadIntegracion> CAPACIDADES = EnumSet.of(
            CapacidadIntegracion.SYNC_CLIENTES,
            CapacidadIntegracion.SYNC_PRODUCTOS,
            CapacidadIntegracion.EMITIR_FACTURA,
            CapacidadIntegracion.CONSULTAR_ESTADO,
            CapacidadIntegracion.RECIBIR_WEBHOOK);

    private final Function<Duration, HttpClient> fabricaHttp;
    private final ObjectMapper mapper;

    @Autowired
    public AdaptadorNetSuite() {
        this(duracion -> HttpClient.newBuilder().connectTimeout(duracion).build(), new ObjectMapper());
    }

    public AdaptadorNetSuite(Function<Duration, HttpClient> fabricaHttp, ObjectMapper mapper) {
        this.fabricaHttp = fabricaHttp;
        this.mapper = mapper;
    }

    @Override
    public ProveedorIntegracion proveedor() {
        return ProveedorIntegracion.NETSUITE;
    }

    @Override
    public Set<CapacidadIntegracion> capacidades() {
        return Set.copyOf(CAPACIDADES);
    }

    @Override
    public boolean configurada(DatosConexionIntegracion datos) {
        return datos != null && presente(datos.credencial("accountId")) && presente(datos.credencial("consumerKey"))
                && presente(datos.credencial("consumerSecret")) && presente(datos.credencial("tokenId"))
                && presente(datos.credencial("tokenSecret"));
    }

    @Override
    public ResultadoIntegracion probarConexion(DatosConexionIntegracion datos) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("NetSuite sin credenciales mínimas.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("Identificador de cuenta de NetSuite inválido.");
        }
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/customer", "GET", null, "limit=1", datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        if (respuesta.cuerpo() == null) {
            return ResultadoIntegracion.fallo("Respuesta inválida de NetSuite.");
        }
        return ResultadoIntegracion.ok("Conexión con NetSuite verificada.", "netsuite-" + datos.credencial("accountId"));
    }

    @Override
    public ResultadoIntegracion sincronizarCliente(DatosConexionIntegracion datos, ClienteIntegracion cliente) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("NetSuite sin credenciales mínimas.");
        }
        if (cliente == null || !presente(cliente.razonSocial())) {
            return ResultadoIntegracion.fallo("Datos de cliente incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("Identificador de cuenta de NetSuite inválido.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("companyName", cliente.razonSocial());
        poner(nodo, "entityId", presente(cliente.nit()) ? cliente.nit() : cliente.idExterno());
        poner(nodo, "email", cliente.email());
        poner(nodo, "phone", cliente.telefono());
        if (presente(cliente.direccion())) {
            nodo.putArray("addressbook").addObject().putObject("value").put("address1", cliente.direccion());
        }
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/customer", "POST", nodo.toString(), null, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = idCreado(respuesta);
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de NetSuite.");
        }
        return ResultadoIntegracion.ok("Cliente sincronizado en NetSuite.", id);
    }

    @Override
    public ResultadoIntegracion sincronizarProducto(DatosConexionIntegracion datos, ProductoIntegracion producto) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("NetSuite sin credenciales mínimas.");
        }
        if (producto == null || !presente(producto.sku()) || !presente(producto.nombre())
                || producto.precio() == null) {
            return ResultadoIntegracion.fallo("Datos de producto incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("Identificador de cuenta de NetSuite inválido.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("itemid", producto.sku());
        nodo.put("displayname", producto.nombre());
        nodo.put("baseprice", producto.precio());
        if (presente(producto.impuesto())) {
            nodo.putObject("taxschedule").put("id", producto.impuesto());
        }
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/item", "POST", nodo.toString(), null, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = idCreado(respuesta);
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de NetSuite.");
        }
        return ResultadoIntegracion.ok("Producto sincronizado en NetSuite.", id);
    }

    @Override
    public ResultadoIntegracion enviarFactura(DatosConexionIntegracion datos, FacturaIntegracion factura) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("NetSuite sin credenciales mínimas.");
        }
        if (factura == null || !presente(factura.clienteId()) || !presente(factura.numero())
                || factura.total() == null) {
            return ResultadoIntegracion.fallo("Datos de factura incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("Identificador de cuenta de NetSuite inválido.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("tranId", factura.numero());
        nodo.putObject("entity").put("id", factura.clienteId());
        if (factura.fecha() != null) {
            nodo.put("tranDate", factura.fecha().toString());
        }
        if (presente(factura.moneda())) {
            nodo.putObject("currency").put("name", factura.moneda());
        }
        nodo.put("total", factura.total());
        ArrayNode lineas = nodo.putObject("item").putArray("items");
        if (factura.lineas() != null) {
            for (LineaFacturaIntegracion linea : factura.lineas()) {
                ObjectNode item = lineas.addObject();
                if (presente(linea.productoId())) {
                    item.putObject("item").put("id", linea.productoId());
                }
                poner(item, "description", linea.descripcion());
                item.put("quantity", linea.cantidad() == null ? BigDecimal.ONE : linea.cantidad());
                item.put("rate", linea.valorUnitario() == null ? BigDecimal.ZERO : linea.valorUnitario());
                if (linea.impuesto() != null && linea.impuesto().compareTo(BigDecimal.ZERO) != 0) {
                    item.put("amount", linea.impuesto());
                }
            }
        }
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/invoice", "POST", nodo.toString(), null, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = idCreado(respuesta);
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de NetSuite.");
        }
        return ResultadoIntegracion.ok("Factura emitida en NetSuite.", id);
    }

    @Override
    public ResultadoIntegracion procesarWebhook(DatosConexionIntegracion datos, EventoIntegracion evento) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("NetSuite sin credenciales mínimas.");
        }
        if (evento == null) {
            return ResultadoIntegracion.fallo("Evento de webhook inválido.");
        }
        if (evento.headers() != null && !evento.headers().isEmpty() && !verificarFirma(datos, evento)) {
            return ResultadoIntegracion.fallo("Firma de webhook inválida.");
        }
        JsonNode raiz;
        try {
            raiz = mapper.readTree(evento.payload() == null ? "" : evento.payload());
        } catch (Exception ex) {
            return ResultadoIntegracion.fallo("Payload de webhook inválido.");
        }
        if (raiz == null || !raiz.isObject()) {
            return ResultadoIntegracion.fallo("Payload de webhook inválido.");
        }
        String id = primeraTexto(raiz, "id", "idExterno", "internalId", "tranId", "entityId");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Webhook sin identificador.");
        }
        String entidad;
        if (raiz.has("total") || raiz.has("tranId") || raiz.has("tranDate")) {
            entidad = "FACTURA";
        } else if (raiz.has("companyName") || raiz.has("entityId") || raiz.has("email")) {
            entidad = "CLIENTE";
        } else if (raiz.has("itemid") || raiz.has("displayname") || raiz.has("baseprice")) {
            entidad = "PRODUCTO";
        } else {
            entidad = "EVENTO";
        }
        return new ResultadoIntegracion(true, "Webhook de " + entidad.toLowerCase(Locale.ROOT)
                + " procesado.", id, "EXITOSO", Map.of("entidad", entidad));
    }

    private RespuestaHttp ejecutar(String url, String metodo, String cuerpo, String query,
                                   DatosConexionIntegracion datos) {
        String contenido;
        HttpHeaders cabeceras;
        try {
            String autorizacion = autorizacion(metodo, url, query, datos);
            HttpClient cliente = fabricaHttp.apply(Duration.ofMillis(timeout(datos, "connectTimeoutMs", "5000")));
            HttpRequest.Builder peticion = HttpRequest.newBuilder(
                            URI.create(url + (presente(query) ? "?" + query : "")))
                    .timeout(Duration.ofMillis(timeout(datos, "readTimeoutMs", "10000")))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", autorizacion);
            if ("POST".equals(metodo)) {
                peticion.POST(HttpRequest.BodyPublishers.ofString(cuerpo == null ? "{}" : cuerpo,
                        StandardCharsets.UTF_8));
            } else {
                peticion.GET();
            }
            HttpResponse<String> respuesta = cliente.send(peticion.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                return new RespuestaHttp(null, null, "NetSuite devolvió un error HTTP.");
            }
            contenido = respuesta.body();
            cabeceras = respuesta.headers();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new RespuestaHttp(null, null, "Operación interrumpida.");
        } catch (IOException | RuntimeException ex) {
            return new RespuestaHttp(null, null, "No se pudo comunicar con NetSuite.");
        }
        try {
            return new RespuestaHttp(mapper.readTree(contenido), cabeceras, null);
        } catch (Exception ex) {
            return new RespuestaHttp(null, cabeceras, "Respuesta inválida de NetSuite.");
        }
    }

    private static String autorizacion(String metodo, String url, String query,
                                       DatosConexionIntegracion datos) {
        Map<String, String> parametros = new TreeMap<>();
        parametros.put("oauth_consumer_key", datos.credencial("consumerKey"));
        parametros.put("oauth_nonce", UUID.randomUUID().toString().replace("-", ""));
        parametros.put("oauth_signature_method", "HMAC-SHA256");
        parametros.put("oauth_timestamp", String.valueOf(Instant.now().getEpochSecond()));
        parametros.put("oauth_token", datos.credencial("tokenId"));
        parametros.put("oauth_version", "1.0");
        if (presente(query)) {
            for (String par : query.split("&")) {
                int separador = par.indexOf('=');
                String clave = separador < 0 ? par : par.substring(0, separador);
                String valor = separador < 0 ? "" : par.substring(separador + 1);
                parametros.put(URLDecoder.decode(clave, StandardCharsets.UTF_8),
                        URLDecoder.decode(valor, StandardCharsets.UTF_8));
            }
        }
        StringBuilder baseString = new StringBuilder(metodo.toUpperCase(Locale.ROOT))
                .append('&').append(porcentaje(url)).append('&');
        StringBuilder normalizados = new StringBuilder();
        for (Map.Entry<String, String> entrada : parametros.entrySet()) {
            if (normalizados.length() > 0) {
                normalizados.append('&');
            }
            normalizados.append(porcentaje(entrada.getKey())).append('=')
                    .append(porcentaje(entrada.getValue()));
        }
        baseString.append(porcentaje(normalizados.toString()));
        String claveFirma = porcentaje(datos.credencial("consumerSecret")) + "&"
                + porcentaje(datos.credencial("tokenSecret"));
        String firma;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(claveFirma.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            firma = Base64.getEncoder().encodeToString(
                    mac.doFinal(baseString.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar la petición de NetSuite", ex);
        }
        StringBuilder cabecera = new StringBuilder("OAuth realm=\"")
                .append(porcentaje(datos.credencial("accountId"))).append('"');
        for (Map.Entry<String, String> entrada : parametros.entrySet()) {
            cabecera.append(", ").append(entrada.getKey()).append("=\"")
                    .append(porcentaje(entrada.getValue())).append('"');
        }
        return cabecera.append(", oauth_signature=\"").append(porcentaje(firma)).append('"').toString();
    }

    private static String base(DatosConexionIntegracion datos) {
        String cuenta = datos.credencial("accountId").trim();
        if (!cuenta.matches("[A-Za-z0-9-]+")) {
            return null;
        }
        return "https://" + cuenta + ".suitetalk.api.netsuite.com";
    }

    private static String idCreado(RespuestaHttp respuesta) {
        if (respuesta.cabeceras() != null) {
            String ubicacion = respuesta.cabeceras().firstValue("Location").orElse(null);
            if (presente(ubicacion)) {
                String ultimo = ubicacion.substring(ubicacion.lastIndexOf('/') + 1);
                if (presente(ultimo)) {
                    return ultimo;
                }
            }
        }
        return primeraTexto(respuesta.cuerpo(), "id", "internalId");
    }

    private static long timeout(DatosConexionIntegracion datos, String clave, String defecto) {
        long valor;
        try {
            valor = Long.parseLong(datos.opcion(clave, defecto));
        } catch (NumberFormatException ex) {
            valor = Long.parseLong(defecto);
        }
        return Math.max(200L, Math.min(30_000L, valor));
    }

    private static String porcentaje(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("~", "%7E");
    }

    private static void poner(ObjectNode nodo, String clave, String valor) {
        if (presente(valor)) {
            nodo.put(clave, valor);
        }
    }

    private static String texto(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        String resultado = valor.asText();
        return resultado.isBlank() ? null : resultado;
    }

    private static String primeraTexto(JsonNode nodo, String... campos) {
        for (String campo : campos) {
            String valor = texto(nodo, campo);
            if (presente(valor)) {
                return valor;
            }
        }
        return null;
    }

    private static boolean presente(String valor) {
        return valor != null && !valor.isBlank();
    }

    private record RespuestaHttp(JsonNode cuerpo, HttpHeaders cabeceras, String error) {
    }
}
