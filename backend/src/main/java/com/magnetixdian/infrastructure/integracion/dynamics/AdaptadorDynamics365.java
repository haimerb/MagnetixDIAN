package com.magnetixdian.infrastructure.integracion.dynamics;

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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
public class AdaptadorDynamics365 implements AdaptadorIntegracion {

    private static final String BASE_API = "/api/data/v9.2";
    private static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com";
    private static final Set<CapacidadIntegracion> CAPACIDADES = EnumSet.of(
            CapacidadIntegracion.SYNC_CLIENTES,
            CapacidadIntegracion.SYNC_PRODUCTOS,
            CapacidadIntegracion.EMITIR_FACTURA,
            CapacidadIntegracion.CONSULTAR_ESTADO,
            CapacidadIntegracion.RECIBIR_WEBHOOK);

    private final Function<Duration, HttpClient> fabricaHttp;
    private final ObjectMapper mapper;

    @Autowired
    public AdaptadorDynamics365() {
        this(duracion -> HttpClient.newBuilder().connectTimeout(duracion).build(), new ObjectMapper());
    }

    public AdaptadorDynamics365(Function<Duration, HttpClient> fabricaHttp, ObjectMapper mapper) {
        this.fabricaHttp = fabricaHttp;
        this.mapper = mapper;
    }

    @Override
    public ProveedorIntegracion proveedor() {
        return ProveedorIntegracion.DYNAMICS_365;
    }

    @Override
    public Set<CapacidadIntegracion> capacidades() {
        return Set.copyOf(CAPACIDADES);
    }

    @Override
    public boolean configurada(DatosConexionIntegracion datos) {
        return datos != null && presente(datos.credencial("url")) && presente(datos.credencial("tenantId"))
                && presente(datos.credencial("clientId")) && presente(datos.credencial("clientSecret"));
    }

    @Override
    public ResultadoIntegracion probarConexion(DatosConexionIntegracion datos) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Dynamics 365 sin credenciales mínimas.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Dynamics 365 inválida.");
        }
        String token = token(datos, base);
        if (token == null) {
            return ResultadoIntegracion.fallo("No se pudo autenticar con Dynamics 365.");
        }
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/WhoAmI", "GET", null, token,
                "application/json", datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = primeraTexto(respuesta.cuerpo(), "UserId", "BusinessUnitId", "OrganizationId");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Dynamics 365.");
        }
        return ResultadoIntegracion.ok("Conexión con Dynamics 365 verificada.", id);
    }

    @Override
    public ResultadoIntegracion sincronizarCliente(DatosConexionIntegracion datos, ClienteIntegracion cliente) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Dynamics 365 sin credenciales mínimas.");
        }
        if (cliente == null || !presente(cliente.razonSocial())) {
            return ResultadoIntegracion.fallo("Datos de cliente incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Dynamics 365 inválida.");
        }
        String token = token(datos, base);
        if (token == null) {
            return ResultadoIntegracion.fallo("No se pudo autenticar con Dynamics 365.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("name", cliente.razonSocial());
        poner(nodo, "accountnumber", cliente.nit());
        poner(nodo, "emailaddress1", cliente.email());
        poner(nodo, "telephone1", cliente.telefono());
        poner(nodo, "address1_line1", cliente.direccion());
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/accounts", "POST", nodo.toString(), token,
                "application/json", datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "accountid");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Dynamics 365.");
        }
        return ResultadoIntegracion.ok("Cliente sincronizado en Dynamics 365.", id);
    }

    @Override
    public ResultadoIntegracion sincronizarProducto(DatosConexionIntegracion datos, ProductoIntegracion producto) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Dynamics 365 sin credenciales mínimas.");
        }
        if (producto == null || !presente(producto.nombre()) || !presente(producto.sku())
                || producto.precio() == null) {
            return ResultadoIntegracion.fallo("Datos de producto incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Dynamics 365 inválida.");
        }
        String token = token(datos, base);
        if (token == null) {
            return ResultadoIntegracion.fallo("No se pudo autenticar con Dynamics 365.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("name", producto.nombre());
        nodo.put("productnumber", producto.sku());
        nodo.put("price", producto.precio());
        poner(nodo, "description", producto.impuesto());
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/items", "POST", nodo.toString(), token,
                "application/json", datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "itemid");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Dynamics 365.");
        }
        return ResultadoIntegracion.ok("Producto sincronizado en Dynamics 365.", id);
    }

    @Override
    public ResultadoIntegracion enviarFactura(DatosConexionIntegracion datos, FacturaIntegracion factura) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Dynamics 365 sin credenciales mínimas.");
        }
        if (factura == null || !presente(factura.clienteId()) || !presente(factura.numero())
                || factura.total() == null) {
            return ResultadoIntegracion.fallo("Datos de factura incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Dynamics 365 inválida.");
        }
        String token = token(datos, base);
        if (token == null) {
            return ResultadoIntegracion.fallo("No se pudo autenticar con Dynamics 365.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("name", factura.numero());
        nodo.put("customerid_account@odata.bind", "/accounts(" + factura.clienteId() + ")");
        if (factura.fecha() != null) {
            nodo.put("issuedate", factura.fecha().toString());
        }
        nodo.put("totalamount", factura.total());
        nodo.put("transactioncurrencyid@odata.bind", "/transactioncurrencies("
                + idMoneda(factura.moneda()) + ")");
        ArrayNode lineas = nodo.putArray("invoice_details");
        if (factura.lineas() != null) {
            for (LineaFacturaIntegracion linea : factura.lineas()) {
                ObjectNode item = lineas.addObject();
                poner(item, "productdescription", linea.descripcion());
                poner(item, "productid", linea.productoId());
                item.put("quantity", linea.cantidad() == null ? BigDecimal.ONE : linea.cantidad());
                item.put("unitprice", linea.valorUnitario() == null ? BigDecimal.ZERO : linea.valorUnitario());
                if (linea.impuesto() != null && linea.impuesto().compareTo(BigDecimal.ZERO) != 0) {
                    item.put("tax", linea.impuesto());
                }
            }
        }
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/invoices", "POST", nodo.toString(), token,
                "application/json", datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "invoiceid");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Dynamics 365.");
        }
        return ResultadoIntegracion.ok("Factura emitida en Dynamics 365.", id);
    }

    @Override
    public ResultadoIntegracion procesarWebhook(DatosConexionIntegracion datos, EventoIntegracion evento) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Dynamics 365 sin credenciales mínimas.");
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
        String id = primeraTexto(raiz, "id", "idExterno", "accountid", "itemid", "invoiceid");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Webhook sin identificador.");
        }
        String entidad;
        if (raiz.has("totalamount") || raiz.has("invoiceid") || raiz.has("invoice_details")) {
            entidad = "FACTURA";
        } else if (raiz.has("accountnumber") || raiz.has("emailaddress1") || raiz.has("accountid")) {
            entidad = "CLIENTE";
        } else if (raiz.has("productnumber") || raiz.has("price") || raiz.has("itemid")) {
            entidad = "PRODUCTO";
        } else {
            entidad = "EVENTO";
        }
        return new ResultadoIntegracion(true, "Webhook de " + entidad.toLowerCase(Locale.ROOT)
                + " procesado.", id, "EXITOSO", Map.of("entidad", entidad));
    }

    private String token(DatosConexionIntegracion datos, String base) {
        Map<String, String> formulario = new LinkedHashMap<>();
        formulario.put("grant_type", "client_credentials");
        formulario.put("client_id", datos.credencial("clientId"));
        formulario.put("client_secret", datos.credencial("clientSecret"));
        formulario.put("scope", datos.opcion("scope", base + "/.default"));
        String url = TOKEN_ENDPOINT + "/" + datos.credencial("tenantId").trim() + "/oauth2/v2.0/token";
        RespuestaHttp respuesta = ejecutar(url, "POST", formulario(formulario), null,
                "application/x-www-form-urlencoded", datos);
        if (respuesta.error() != null) {
            return null;
        }
        String accessToken = texto(respuesta.cuerpo(), "access_token");
        return presente(accessToken) ? accessToken : null;
    }

    private RespuestaHttp ejecutar(String url, String metodo, String cuerpo, String bearer,
                                   String contentType, DatosConexionIntegracion datos) {
        String contenido;
        HttpHeaders cabeceras;
        try {
            HttpClient cliente = fabricaHttp.apply(Duration.ofMillis(timeout(datos, "connectTimeoutMs", "5000")));
            HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeout(datos, "readTimeoutMs", "10000")))
                    .header("Accept", "application/json")
                    .header("Content-Type", contentType)
                    .header("OData-MaxVersion", "4.0")
                    .header("OData-Version", "4.0")
                    .header("Prefer", "return=representation");
            if (presente(bearer)) {
                peticion.header("Authorization", "Bearer " + bearer);
            }
            if ("POST".equals(metodo)) {
                peticion.POST(HttpRequest.BodyPublishers.ofString(cuerpo == null ? "" : cuerpo,
                        StandardCharsets.UTF_8));
            } else {
                peticion.GET();
            }
            HttpResponse<String> respuesta = cliente.send(peticion.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                return new RespuestaHttp(null, null, "Dynamics 365 devolvió un error HTTP.");
            }
            contenido = respuesta.body();
            cabeceras = respuesta.headers();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new RespuestaHttp(null, null, "Operación interrumpida.");
        } catch (IOException | RuntimeException ex) {
            return new RespuestaHttp(null, null, "No se pudo comunicar con Dynamics 365.");
        }
        try {
            return new RespuestaHttp(mapper.readTree(contenido), cabeceras, null);
        } catch (Exception ex) {
            return new RespuestaHttp(null, cabeceras, "Respuesta inválida de Dynamics 365.");
        }
    }

    private static String formulario(Map<String, String> valores) {
        List<String> pares = new ArrayList<>();
        for (Map.Entry<String, String> entrada : valores.entrySet()) {
            pares.add(codificar(entrada.getKey()) + "=" + codificar(entrada.getValue()));
        }
        return String.join("&", pares);
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }

    private static String base(DatosConexionIntegracion datos) {
        String valor = datos.credencial("url");
        if (!presente(valor)) {
            return null;
        }
        String url = valor.trim();
        if (!url.contains("://")) {
            url = "https://" + url;
        }
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                return null;
            }
            return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String idMoneda(String moneda) {
        if ("USD".equalsIgnoreCase(moneda)) {
            return "1e8f21ee-6ba0-ed11-aad0-0022489a5b53";
        }
        if ("EUR".equalsIgnoreCase(moneda)) {
            return "2e8f21ee-6ba0-ed11-aad0-0022489a5b53";
        }
        return "3e8f21ee-6ba0-ed11-aad0-0022489a5b53";
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
