package com.magnetixdian.infrastructure.integracion.sap;

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
import java.net.http.HttpHeaders;
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
public class AdaptadorSapBusinessOne implements AdaptadorIntegracion {

    private static final String BASE_API = "/b1s/v1";
    private static final Set<CapacidadIntegracion> CAPACIDADES = EnumSet.of(
            CapacidadIntegracion.SYNC_CLIENTES,
            CapacidadIntegracion.SYNC_PRODUCTOS,
            CapacidadIntegracion.EMITIR_FACTURA,
            CapacidadIntegracion.CONSULTAR_ESTADO,
            CapacidadIntegracion.RECIBIR_WEBHOOK);

    private final Function<Duration, HttpClient> fabricaHttp;
    private final ObjectMapper mapper;

    @Autowired
    public AdaptadorSapBusinessOne() {
        this(duracion -> HttpClient.newBuilder().connectTimeout(duracion).build(), new ObjectMapper());
    }

    public AdaptadorSapBusinessOne(Function<Duration, HttpClient> fabricaHttp, ObjectMapper mapper) {
        this.fabricaHttp = fabricaHttp;
        this.mapper = mapper;
    }

    @Override
    public ProveedorIntegracion proveedor() {
        return ProveedorIntegracion.SAP_BUSINESS_ONE;
    }

    @Override
    public Set<CapacidadIntegracion> capacidades() {
        return Set.copyOf(CAPACIDADES);
    }

    @Override
    public boolean configurada(DatosConexionIntegracion datos) {
        return datos != null && presente(datos.credencial("url")) && presente(datos.credencial("companyDB"))
                && presente(datos.credencial("username")) && presente(datos.credencial("password"));
    }

    @Override
    public ResultadoIntegracion probarConexion(DatosConexionIntegracion datos) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("SAP Business One sin credenciales mínimas.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de SAP Business One inválida.");
        }
        Sesion sesion = autenticar(datos, base);
        if (sesion.error() != null) {
            return ResultadoIntegracion.fallo(sesion.error());
        }
        String nombre = texto(sesion.cuerpo(), "CompanyName");
        return ResultadoIntegracion.ok("Conexión con SAP Business One verificada.",
                presente(nombre) ? nombre : datos.credencial("companyDB"));
    }

    @Override
    public ResultadoIntegracion sincronizarCliente(DatosConexionIntegracion datos, ClienteIntegracion cliente) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("SAP Business One sin credenciales mínimas.");
        }
        if (cliente == null || !presente(cliente.razonSocial())) {
            return ResultadoIntegracion.fallo("Datos de cliente incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de SAP Business One inválida.");
        }
        Sesion sesion = autenticar(datos, base);
        if (sesion.error() != null) {
            return ResultadoIntegracion.fallo(sesion.error());
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("CardCode", presente(cliente.nit()) ? cliente.nit() : cliente.idExterno());
        nodo.put("CardName", cliente.razonSocial());
        nodo.put("CardType", "c");
        poner(nodo, "EmailAddress", cliente.email());
        poner(nodo, "Phone", cliente.telefono());
        poner(nodo, "Address", cliente.direccion());
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/BusinessPartners", "POST", nodo.toString(),
                sesion, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "CardCode");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de SAP Business One.");
        }
        return ResultadoIntegracion.ok("Cliente sincronizado en SAP Business One.", id);
    }

    @Override
    public ResultadoIntegracion sincronizarProducto(DatosConexionIntegracion datos, ProductoIntegracion producto) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("SAP Business One sin credenciales mínimas.");
        }
        if (producto == null || !presente(producto.sku()) || !presente(producto.nombre())
                || producto.precio() == null) {
            return ResultadoIntegracion.fallo("Datos de producto incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de SAP Business One inválida.");
        }
        Sesion sesion = autenticar(datos, base);
        if (sesion.error() != null) {
            return ResultadoIntegracion.fallo(sesion.error());
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("ItemCode", producto.sku());
        nodo.put("ItemName", producto.nombre());
        nodo.put("SalesUnit", "UN");
        poner(nodo, "Description", producto.impuesto());
        ObjectNode precio = nodo.putArray("Prices").addObject();
        precio.put("PriceList", 1);
        precio.put("Price", producto.precio());
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/Items", "POST", nodo.toString(), sesion, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "ItemCode");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de SAP Business One.");
        }
        return ResultadoIntegracion.ok("Producto sincronizado en SAP Business One.", id);
    }

    @Override
    public ResultadoIntegracion enviarFactura(DatosConexionIntegracion datos, FacturaIntegracion factura) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("SAP Business One sin credenciales mínimas.");
        }
        if (factura == null || !presente(factura.clienteId()) || !presente(factura.clienteNit())
                || factura.total() == null) {
            return ResultadoIntegracion.fallo("Datos de factura incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de SAP Business One inválida.");
        }
        Sesion sesion = autenticar(datos, base);
        if (sesion.error() != null) {
            return ResultadoIntegracion.fallo(sesion.error());
        }
        ObjectNode nodo = mapper.createObjectNode();
        nodo.put("CardCode", factura.clienteId());
        poner(nodo, "DocumentNumber", factura.numero());
        if (factura.fecha() != null) {
            nodo.put("DocDate", factura.fecha().toString());
        }
        nodo.put("DocCurrency", presente(factura.moneda()) ? factura.moneda() : "COP");
        nodo.put("DocTotal", factura.total());
        ArrayNode lineas = nodo.putArray("DocumentLines");
        if (factura.lineas() != null) {
            for (LineaFacturaIntegracion linea : factura.lineas()) {
                ObjectNode item = lineas.addObject();
                poner(item, "ItemCode", linea.productoId());
                poner(item, "Description", linea.descripcion());
                item.put("Quantity", linea.cantidad() == null ? BigDecimal.ONE : linea.cantidad());
                item.put("Price", linea.valorUnitario() == null ? BigDecimal.ZERO : linea.valorUnitario());
                if (linea.impuesto() != null && linea.impuesto().compareTo(BigDecimal.ZERO) != 0) {
                    item.put("TaxCode", "IVA");
                }
            }
        }
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/Invoices", "POST", nodo.toString(), sesion, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "DocEntry");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de SAP Business One.");
        }
        return ResultadoIntegracion.ok("Factura emitida en SAP Business One.", id);
    }

    @Override
    public ResultadoIntegracion procesarWebhook(DatosConexionIntegracion datos, EventoIntegracion evento) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("SAP Business One sin credenciales mínimas.");
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
        String id = primeraTexto(raiz, "id", "idExterno", "DocEntry", "CardCode", "ItemCode");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Webhook sin identificador.");
        }
        String entidad;
        if (raiz.has("DocTotal") || raiz.has("DocumentLines") || raiz.has("DocEntry")) {
            entidad = "FACTURA";
        } else if (raiz.has("CardName") || raiz.has("CardCode") || raiz.has("EmailAddress")) {
            entidad = "CLIENTE";
        } else if (raiz.has("ItemCode") || raiz.has("ItemName") || raiz.has("Prices")) {
            entidad = "PRODUCTO";
        } else {
            entidad = "EVENTO";
        }
        return new ResultadoIntegracion(true, "Webhook de " + entidad.toLowerCase(Locale.ROOT)
                + " procesado.", id, "EXITOSO", Map.of("entidad", entidad));
    }

    private Sesion autenticar(DatosConexionIntegracion datos, String base) {
        ObjectNode credenciales = mapper.createObjectNode();
        credenciales.put("CompanyDB", datos.credencial("companyDB"));
        credenciales.put("UserName", datos.credencial("username"));
        credenciales.put("Password", datos.credencial("password"));
        RespuestaHttp respuesta = ejecutar(base + BASE_API + "/Login", "POST", credenciales.toString(),
                null, datos);
        if (respuesta.error() != null) {
            return new Sesion(null, null, null, respuesta.error());
        }
        String sessionId = texto(respuesta.cuerpo(), "SessionId");
        if (!presente(sessionId)) {
            return new Sesion(null, null, null, "Respuesta inválida de SAP Business One.");
        }
        return new Sesion(sessionId, routeId(respuesta.cabeceras()), respuesta.cuerpo(), null);
    }

    private RespuestaHttp ejecutar(String url, String metodo, String cuerpo, Sesion sesion,
                                   DatosConexionIntegracion datos) {
        String contenido;
        HttpHeaders cabeceras;
        try {
            HttpClient cliente = fabricaHttp.apply(Duration.ofMillis(timeout(datos, "connectTimeoutMs", "5000")));
            HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeout(datos, "readTimeoutMs", "10000")))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "odata.maxpagesize=1");
            if (sesion != null && presente(sesion.sessionId())) {
                String cookie = "B1SESSION=" + sesion.sessionId();
                if (presente(sesion.routeId())) {
                    cookie += "; ROUTEID=" + sesion.routeId();
                }
                peticion.header("Cookie", cookie);
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
                return new RespuestaHttp(null, null, "SAP Business One devolvió un error HTTP.");
            }
            contenido = respuesta.body();
            cabeceras = respuesta.headers();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new RespuestaHttp(null, null, "Operación interrumpida.");
        } catch (IOException | RuntimeException ex) {
            return new RespuestaHttp(null, null, "No se pudo comunicar con SAP Business One.");
        }
        try {
            return new RespuestaHttp(mapper.readTree(contenido), cabeceras, null);
        } catch (Exception ex) {
            return new RespuestaHttp(null, cabeceras, "Respuesta inválida de SAP Business One.");
        }
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

    private static String routeId(HttpHeaders cabeceras) {
        if (cabeceras == null) {
            return null;
        }
        String valor = cabeceras.firstValue("Set-Cookie").orElse(null);
        if (!presente(valor)) {
            return null;
        }
        int inicio = valor.indexOf("ROUTEID=");
        if (inicio < 0) {
            return null;
        }
        inicio += "ROUTEID=".length();
        int fin = valor.indexOf(';', inicio);
        return fin < 0 ? valor.substring(inicio) : valor.substring(inicio, fin);
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

    private record Sesion(String sessionId, String routeId, JsonNode cuerpo, String error) {
    }

    private record RespuestaHttp(JsonNode cuerpo, HttpHeaders cabeceras, String error) {
    }
}
