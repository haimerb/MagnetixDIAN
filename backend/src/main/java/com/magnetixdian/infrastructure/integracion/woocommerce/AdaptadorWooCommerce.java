package com.magnetixdian.infrastructure.integracion.woocommerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
public class AdaptadorWooCommerce implements AdaptadorIntegracion {

    private static final String BASE_API = "/wp-json/wc/v3";
    private static final Set<CapacidadIntegracion> CAPACIDADES = EnumSet.of(
            CapacidadIntegracion.RECIBIR_WEBHOOK,
            CapacidadIntegracion.SYNC_CLIENTES,
            CapacidadIntegracion.SYNC_PRODUCTOS,
            CapacidadIntegracion.CONSULTAR_ESTADO);

    private final Function<Duration, HttpClient> fabricaHttp;
    private final ObjectMapper mapper;

    @Autowired
    public AdaptadorWooCommerce() {
        this(duracion -> HttpClient.newBuilder().connectTimeout(duracion).build(), new ObjectMapper());
    }

    public AdaptadorWooCommerce(Function<Duration, HttpClient> fabricaHttp, ObjectMapper mapper) {
        this.fabricaHttp = fabricaHttp;
        this.mapper = mapper;
    }

    @Override
    public ProveedorIntegracion proveedor() {
        return ProveedorIntegracion.WOOCOMMERCE;
    }

    @Override
    public Set<CapacidadIntegracion> capacidades() {
        return Set.copyOf(CAPACIDADES);
    }

    @Override
    public boolean configurada(DatosConexionIntegracion datos) {
        return datos != null && presente(datos.credencial("url")) && presente(datos.credencial("consumerKey"))
                && presente(datos.credencial("consumerSecret"));
    }

    @Override
    public ResultadoIntegracion probarConexion(DatosConexionIntegracion datos) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("WooCommerce sin credenciales mínimas.");
        }
        String base = normalizarUrl(datos.credencial("url"));
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de WooCommerce inválida.");
        }
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/system_status", "GET", null,
                autorizacion(datos), datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        JsonNode entorno = respuesta.cuerpo() == null ? null : respuesta.cuerpo().path("environment");
        if (entorno == null || entorno.isMissingNode() || entorno.isNull()) {
            return ResultadoIntegracion.fallo("Respuesta inválida de WooCommerce.");
        }
        String sitio = texto(entorno, "home_url");
        return ResultadoIntegracion.ok("Conexión con WooCommerce verificada.",
                sitio == null ? texto(entorno, "site_url") : sitio);
    }

    @Override
    public ResultadoIntegracion sincronizarCliente(DatosConexionIntegracion datos, ClienteIntegracion cliente) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("WooCommerce sin credenciales mínimas.");
        }
        String base = normalizarUrl(datos.credencial("url"));
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de WooCommerce inválida.");
        }
        if (cliente == null || !presente(cliente.email()) || !presente(cliente.razonSocial())) {
            return ResultadoIntegracion.fallo("Datos de cliente incompletos.");
        }
        String[] nombres = separarNombre(cliente.razonSocial());
        ObjectNode clienteJson = mapper.createObjectNode();
        clienteJson.put("email", cliente.email());
        clienteJson.put("username", presente(cliente.nit()) ? cliente.nit() : cliente.email());
        clienteJson.put("first_name", nombres[0]);
        clienteJson.put("last_name", nombres[1]);
        ObjectNode facturacion = clienteJson.putObject("billing");
        facturacion.put("company", cliente.razonSocial());
        poner(facturacion, "phone", cliente.telefono());
        poner(facturacion, "address_1", cliente.direccion());
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/customers", "POST", clienteJson.toString(),
                autorizacion(datos), datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "id");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de WooCommerce.");
        }
        return ResultadoIntegracion.ok("Cliente sincronizado en WooCommerce.", id);
    }

    @Override
    public ResultadoIntegracion sincronizarProducto(DatosConexionIntegracion datos, ProductoIntegracion producto) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("WooCommerce sin credenciales mínimas.");
        }
        String base = normalizarUrl(datos.credencial("url"));
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de WooCommerce inválida.");
        }
        if (producto == null || !presente(producto.sku()) || !presente(producto.nombre())
                || producto.precio() == null) {
            return ResultadoIntegracion.fallo("Datos de producto incompletos.");
        }
        ObjectNode productoJson = mapper.createObjectNode();
        productoJson.put("name", producto.nombre());
        productoJson.put("sku", producto.sku());
        productoJson.put("regular_price", producto.precio().toPlainString());
        poner(productoJson, "description", producto.impuesto());
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/products", "POST", productoJson.toString(),
                autorizacion(datos), datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "id");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de WooCommerce.");
        }
        return ResultadoIntegracion.ok("Producto sincronizado en WooCommerce.", id);
    }

    @Override
    public ResultadoIntegracion procesarWebhook(DatosConexionIntegracion datos, EventoIntegracion evento) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("WooCommerce sin credenciales mínimas.");
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
        String tema = tema(evento, raiz, "X-WC-Webhook-Topic");
        if (coincide(tema, "customer", "cliente")) {
            ClienteIntegracion cliente = mapearCliente(raiz);
            if (!presente(cliente.idExterno())) {
                return ResultadoIntegracion.fallo("Webhook de cliente sin identificador.");
            }
            return new ResultadoIntegracion(true, "Webhook de cliente procesado.", cliente.idExterno(),
                    "EXITOSO", Map.of("entidad", "CLIENTE"));
        }
        if (coincide(tema, "product", "producto")) {
            ProductoIntegracion producto = mapearProducto(raiz);
            if (!presente(producto.idExterno())) {
                return ResultadoIntegracion.fallo("Webhook de producto sin identificador.");
            }
            return new ResultadoIntegracion(true, "Webhook de producto procesado.", producto.idExterno(),
                    "EXITOSO", Map.of("entidad", "PRODUCTO"));
        }
        if (coincide(tema, "order", "pedido")) {
            FacturaIntegracion factura = mapearFactura(raiz);
            if (!presente(factura.idExterno())) {
                return ResultadoIntegracion.fallo("Webhook de pedido sin identificador.");
            }
            return new ResultadoIntegracion(true, "Webhook de pedido procesado.", factura.idExterno(),
                    "EXITOSO", Map.of("entidad", "FACTURA"));
        }
        return ResultadoIntegracion.fallo("Tema de webhook no soportado.");
    }

    @Override
    public boolean verificarFirma(DatosConexionIntegracion datos, EventoIntegracion evento) {
        String secreto = datos == null ? null : datos.credencial("webhookSecret");
        if (!presente(secreto) || evento == null) {
            return false;
        }
        String firma = cabecera(evento, "X-WC-Webhook-Signature");
        if (!presente(firma)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperado = mac.doFinal((evento.payload() == null ? "" : evento.payload())
                    .getBytes(StandardCharsets.UTF_8));
            byte[] recibido = Base64.getDecoder().decode(firma.trim());
            return MessageDigest.isEqual(esperado, recibido);
        } catch (Exception ex) {
            return false;
        }
    }

    private RespuestaHttp ejecutar(String url, String metodo, String cuerpo,
                                   Map<String, String> cabeceras, DatosConexionIntegracion datos) {
        String contenido;
        try {
            HttpClient cliente = fabricaHttp.apply(Duration.ofMillis(timeout(datos, "connectTimeoutMs", "5000")));
            HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeout(datos, "readTimeoutMs", "10000")))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json");
            cabeceras.forEach(peticion::header);
            if ("POST".equals(metodo)) {
                peticion.POST(HttpRequest.BodyPublishers.ofString(cuerpo == null ? "{}" : cuerpo,
                        StandardCharsets.UTF_8));
            } else {
                peticion.GET();
            }
            HttpResponse<String> respuesta = cliente.send(peticion.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                return new RespuestaHttp(null, "WooCommerce devolvió un error HTTP.");
            }
            contenido = respuesta.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new RespuestaHttp(null, "Operación interrumpida.");
        } catch (IOException | RuntimeException ex) {
            return new RespuestaHttp(null, "No se pudo comunicar con WooCommerce.");
        }
        try {
            return new RespuestaHttp(mapper.readTree(contenido), null);
        } catch (Exception ex) {
            return new RespuestaHttp(null, "Respuesta inválida de WooCommerce.");
        }
    }

    private ClienteIntegracion mapearCliente(JsonNode raiz) {
        JsonNode facturacion = raiz.path("billing");
        String razon = texto(facturacion, "company");
        if (razon == null) {
            razon = nombreCompleto(raiz);
        }
        String via = texto(facturacion, "address_1");
        String ciudad = texto(facturacion, "city");
        String completa = via == null ? null : ciudad == null ? via : via + ", " + ciudad;
        return new ClienteIntegracion(texto(raiz, "id"), texto(raiz, "username"), razon,
                texto(raiz, "email"), texto(facturacion, "phone"), completa);
    }

    private ProductoIntegracion mapearProducto(JsonNode raiz) {
        return new ProductoIntegracion(texto(raiz, "id"), texto(raiz, "sku"), texto(raiz, "name"),
                decimal(raiz, "price"), texto(raiz, "tax_status"));
    }

    private FacturaIntegracion mapearFactura(JsonNode raiz) {
        JsonNode facturacion = raiz.path("billing");
        JsonNode articulos = raiz.path("line_items");
        List<LineaFacturaIntegracion> lineas = new ArrayList<>();
        if (articulos.isArray()) {
            for (JsonNode articulo : articulos) {
                lineas.add(new LineaFacturaIntegracion(texto(articulo, "sku"), texto(articulo, "name"),
                        decimal(articulo, "quantity"), decimal(articulo, "price"), decimal(articulo, "total_tax")));
            }
        }
        return new FacturaIntegracion(texto(raiz, "id"), texto(raiz, "number"), fecha(texto(raiz, "date_created")),
                texto(raiz, "customer_id"), texto(facturacion, "company"), decimal(raiz, "subtotal"),
                decimal(raiz, "total_tax"), decimal(raiz, "total"), texto(raiz, "currency"), lineas,
                Map.of("proveedor", "WOOCOMMERCE"));
    }

    private static Map<String, String> autorizacion(DatosConexionIntegracion datos) {
        String par = datos.credencial("consumerKey") + ":" + datos.credencial("consumerSecret");
        return Map.of("Authorization", "Basic "
                + Base64.getEncoder().encodeToString(par.getBytes(StandardCharsets.UTF_8)));
    }

    private static LocalDate fecha(String valor) {
        if (valor == null || valor.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(valor.substring(0, 10));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String[] separarNombre(String razonSocial) {
        String[] partes = razonSocial.trim().split("\\s+", 2);
        return new String[]{partes[0], partes.length > 1 ? partes[1] : ""};
    }

    private static String nombreCompleto(JsonNode raiz) {
        String nombre = texto(raiz, "first_name");
        String apellido = texto(raiz, "last_name");
        if (nombre == null) {
            return apellido;
        }
        return apellido == null ? nombre : nombre + " " + apellido;
    }

    private static String tema(EventoIntegracion evento, JsonNode raiz, String cabeceraTema) {
        String tema = cabecera(evento, cabeceraTema);
        if (!presente(tema)) {
            tema = evento.tipo();
        }
        if (!presente(tema)) {
            if (raiz.has("line_items")) {
                tema = "order";
            } else if (raiz.has("sku") || raiz.has("regular_price")) {
                tema = "product";
            } else if (raiz.has("email")) {
                tema = "customer";
            }
        }
        return tema;
    }

    private static boolean coincide(String tema, String... claves) {
        if (!presente(tema)) {
            return false;
        }
        String valor = tema.toLowerCase(Locale.ROOT);
        for (String clave : claves) {
            if (valor.contains(clave)) {
                return true;
            }
        }
        return false;
    }

    private static String cabecera(EventoIntegracion evento, String nombre) {
        if (evento.headers() == null) {
            return null;
        }
        String valor = evento.headers().get(nombre);
        if (valor != null) {
            return valor;
        }
        for (Map.Entry<String, String> entrada : evento.headers().entrySet()) {
            if (entrada.getKey() != null && entrada.getKey().equalsIgnoreCase(nombre)) {
                return entrada.getValue();
            }
        }
        return null;
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

    private static BigDecimal decimal(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(valor.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
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

    private static String normalizarUrl(String valor) {
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

    private static boolean presente(String valor) {
        return valor != null && !valor.isBlank();
    }

    private record RespuestaHttp(JsonNode cuerpo, String error) {
    }
}
