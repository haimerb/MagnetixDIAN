package com.magnetixdian.infrastructure.integracion.siigo;

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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
public class AdaptadorSiigo implements AdaptadorIntegracion {

    private static final String URL_DEFAULT = "https://api.siigo.com";
    private static final Set<CapacidadIntegracion> CAPACIDADES = EnumSet.of(
            CapacidadIntegracion.SYNC_CLIENTES,
            CapacidadIntegracion.SYNC_PRODUCTOS,
            CapacidadIntegracion.EMITIR_FACTURA,
            CapacidadIntegracion.CONSULTAR_ESTADO,
            CapacidadIntegracion.RECIBIR_WEBHOOK);

    private final Function<Duration, HttpClient> fabricaHttp;
    private final ObjectMapper mapper;

    @Autowired
    public AdaptadorSiigo() {
        this(duracion -> HttpClient.newBuilder().connectTimeout(duracion).build(), new ObjectMapper());
    }

    public AdaptadorSiigo(Function<Duration, HttpClient> fabricaHttp, ObjectMapper mapper) {
        this.fabricaHttp = fabricaHttp;
        this.mapper = mapper;
    }

    @Override
    public ProveedorIntegracion proveedor() {
        return ProveedorIntegracion.SIIGO;
    }

    @Override
    public Set<CapacidadIntegracion> capacidades() {
        return Set.copyOf(CAPACIDADES);
    }

    @Override
    public boolean configurada(DatosConexionIntegracion datos) {
        return datos != null && presente(datos.credencial("username"))
                && presente(datos.credencial("accessKey"));
    }

    @Override
    public ResultadoIntegracion probarConexion(DatosConexionIntegracion datos) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Siigo sin credenciales mínimas.");
        }
        if (urlBase(datos) == null) {
            return ResultadoIntegracion.fallo("URL de Siigo inválida.");
        }
        RespuestaHttp autenticacion = autenticar(datos);
        if (autenticacion.error() != null) {
            return ResultadoIntegracion.fallo(autenticacion.error());
        }
        if (!presente(token(autenticacion.cuerpo()))) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Siigo.");
        }
        return ResultadoIntegracion.ok("Conexión con Siigo verificada.", texto(autenticacion.cuerpo(), "userId"));
    }

    @Override
    public ResultadoIntegracion sincronizarCliente(DatosConexionIntegracion datos, ClienteIntegracion cliente) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Siigo sin credenciales mínimas.");
        }
        String base = urlBase(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Siigo inválida.");
        }
        if (cliente == null || !presente(cliente.razonSocial())) {
            return ResultadoIntegracion.fallo("Datos de cliente incompletos.");
        }
        String bearer = token(datos);
        if (bearer == null) {
            return ResultadoIntegracion.fallo("No se pudo autenticar con Siigo.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("type", "Customer");
        poner(nodo, "identification", cliente.nit());
        ArrayNode nombres = nodo.putArray("name");
        nombres.add(cliente.razonSocial());
        poner(nodo, "email", cliente.email());
        poner(nodo, "phone", cliente.telefono());
        if (presente(cliente.direccion())) {
            nodo.putObject("address").put("address", cliente.direccion());
        }
        RespuestaHttp respuesta = ejecutar(base + "/v1/customers", "POST", nodo.toString(), bearer, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "id");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Siigo.");
        }
        return ResultadoIntegracion.ok("Cliente sincronizado en Siigo.", id);
    }

    @Override
    public ResultadoIntegracion sincronizarProducto(DatosConexionIntegracion datos, ProductoIntegracion producto) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Siigo sin credenciales mínimas.");
        }
        String base = urlBase(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Siigo inválida.");
        }
        if (producto == null || !presente(producto.sku()) || !presente(producto.nombre())
                || producto.precio() == null) {
            return ResultadoIntegracion.fallo("Datos de producto incompletos.");
        }
        String bearer = token(datos);
        if (bearer == null) {
            return ResultadoIntegracion.fallo("No se pudo autenticar con Siigo.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("code", producto.sku());
        nodo.put("name", producto.nombre());
        poner(nodo, "description", producto.impuesto());
        ObjectNode precio = nodo.putArray("prices").addObject();
        precio.put("price_list_id", 1);
        precio.put("value", producto.precio().toPlainString());
        RespuestaHttp respuesta = ejecutar(base + "/v1/products", "POST", nodo.toString(), bearer, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "id");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Siigo.");
        }
        return ResultadoIntegracion.ok("Producto sincronizado en Siigo.", id);
    }

    @Override
    public ResultadoIntegracion enviarFactura(DatosConexionIntegracion datos, FacturaIntegracion factura) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Siigo sin credenciales mínimas.");
        }
        String base = urlBase(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Siigo inválida.");
        }
        if (factura == null || !presente(factura.clienteNit()) || factura.total() == null) {
            return ResultadoIntegracion.fallo("Datos de factura incompletos.");
        }
        String bearer = token(datos);
        if (bearer == null) {
            return ResultadoIntegracion.fallo("No se pudo autenticar con Siigo.");
        }
        RespuestaHttp respuesta = ejecutar(base + "/v1/invoices", "POST", payloadFactura(factura).toString(),
                bearer, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "id");
        if (!presente(id)) {
            id = texto(respuesta.cuerpo(), "invoice_id");
        }
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Siigo.");
        }
        return ResultadoIntegracion.ok("Factura emitida en Siigo.", id);
    }

    @Override
    public ResultadoIntegracion procesarWebhook(DatosConexionIntegracion datos, EventoIntegracion evento) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Siigo sin credenciales mínimas.");
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
        String id = texto(raiz, "id");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Webhook sin identificador.");
        }
        String entidad;
        if (raiz.has("items") || raiz.has("totals") || raiz.has("customer")) {
            entidad = "FACTURA";
        } else if (raiz.has("identification") || raiz.has("email")) {
            entidad = "CLIENTE";
        } else if (raiz.has("code") || raiz.has("prices")) {
            entidad = "PRODUCTO";
        } else {
            entidad = "EVENTO";
        }
        return new ResultadoIntegracion(true, "Webhook de " + entidad.toLowerCase(Locale.ROOT)
                + " procesado.", id, "EXITOSO", Map.of("entidad", entidad));
    }

    private ObjectNode payloadFactura(FacturaIntegracion factura) {
        ObjectNode nodo = mapper.createObjectNode();
        ObjectNode cliente = nodo.putObject("customer");
        poner(cliente, "id", factura.clienteId());
        poner(cliente, "identification", factura.clienteNit());
        poner(nodo, "number", factura.numero());
        if (factura.fecha() != null) {
            nodo.put("date", factura.fecha().toString());
        }
        nodo.put("currency", presente(factura.moneda()) ? factura.moneda() : "COP");
        ArrayNode items = nodo.putArray("items");
        if (factura.lineas() != null) {
            for (LineaFacturaIntegracion linea : factura.lineas()) {
                ObjectNode item = items.addObject();
                poner(item, "code", linea.productoId());
                poner(item, "description", linea.descripcion());
                item.put("quantity", importe(linea.cantidad(), "1"));
                item.put("price", importe(linea.valorUnitario(), "0"));
                if (linea.impuesto() != null && linea.impuesto().compareTo(BigDecimal.ZERO) != 0) {
                    ObjectNode impuesto = item.putArray("taxes").addObject();
                    impuesto.put("id", "IVA");
                    impuesto.put("value", linea.impuesto().toPlainString());
                }
            }
        }
        ArrayNode totales = nodo.putArray("totals");
        totales.addObject().put("id", "SUBTOTAL").put("value", importe(factura.subtotal(), "0"));
        totales.addObject().put("id", "IVA").put("value", importe(factura.impuestos(), "0"));
        totales.addObject().put("id", "TOTAL").put("value", importe(factura.total(), "0"));
        return nodo;
    }

    private RespuestaHttp autenticar(DatosConexionIntegracion datos) {
        ObjectNode credenciales = mapper.createObjectNode();
        credenciales.put("username", datos.credencial("username"));
        credenciales.put("access_key", datos.credencial("accessKey"));
        return ejecutar(urlBase(datos) + "/auth", "POST", credenciales.toString(), null, datos);
    }

    private String token(DatosConexionIntegracion datos) {
        RespuestaHttp autenticacion = autenticar(datos);
        return autenticacion.error() == null ? token(autenticacion.cuerpo()) : null;
    }

    private static String token(JsonNode cuerpo) {
        String valor = texto(cuerpo, "access_token");
        return presente(valor) ? valor : texto(cuerpo, "token");
    }

    private RespuestaHttp ejecutar(String url, String metodo, String cuerpo, String bearer,
                                   DatosConexionIntegracion datos) {
        String contenido;
        try {
            HttpClient cliente = fabricaHttp.apply(Duration.ofMillis(timeout(datos, "connectTimeoutMs", "5000")));
            HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeout(datos, "readTimeoutMs", "10000")))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json");
            if (bearer != null) {
                peticion.header("Authorization", "Bearer " + bearer);
            }
            if ("POST".equals(metodo)) {
                peticion.POST(HttpRequest.BodyPublishers.ofString(cuerpo == null ? "{}" : cuerpo,
                        StandardCharsets.UTF_8));
            } else {
                peticion.GET();
            }
            HttpResponse<String> respuesta = cliente.send(peticion.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                return new RespuestaHttp(null, "Siigo devolvió un error HTTP.");
            }
            contenido = respuesta.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new RespuestaHttp(null, "Operación interrumpida.");
        } catch (IOException | RuntimeException ex) {
            return new RespuestaHttp(null, "No se pudo comunicar con Siigo.");
        }
        try {
            return new RespuestaHttp(mapper.readTree(contenido), null);
        } catch (Exception ex) {
            return new RespuestaHttp(null, "Respuesta inválida de Siigo.");
        }
    }

    private static String urlBase(DatosConexionIntegracion datos) {
        String valor = datos.credencial("url");
        return normalizarUrl(presente(valor) ? valor : URL_DEFAULT);
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

    private static String importe(BigDecimal valor, String defecto) {
        return valor == null ? defecto : valor.toPlainString();
    }

    private static boolean presente(String valor) {
        return valor != null && !valor.isBlank();
    }

    private record RespuestaHttp(JsonNode cuerpo, String error) {
    }
}
