package com.magnetixdian.infrastructure.integracion.odoo;

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
public class AdaptadorOdoo implements AdaptadorIntegracion {

    private static final Set<CapacidadIntegracion> CAPACIDADES = EnumSet.of(
            CapacidadIntegracion.SYNC_CLIENTES,
            CapacidadIntegracion.SYNC_PRODUCTOS,
            CapacidadIntegracion.EMITIR_FACTURA,
            CapacidadIntegracion.CONSULTAR_ESTADO,
            CapacidadIntegracion.RECIBIR_WEBHOOK);

    private final Function<Duration, HttpClient> fabricaHttp;
    private final ObjectMapper mapper;

    @Autowired
    public AdaptadorOdoo() {
        this(duracion -> HttpClient.newBuilder().connectTimeout(duracion).build(), new ObjectMapper());
    }

    public AdaptadorOdoo(Function<Duration, HttpClient> fabricaHttp, ObjectMapper mapper) {
        this.fabricaHttp = fabricaHttp;
        this.mapper = mapper;
    }

    @Override
    public ProveedorIntegracion proveedor() {
        return ProveedorIntegracion.ODOO;
    }

    @Override
    public Set<CapacidadIntegracion> capacidades() {
        return Set.copyOf(CAPACIDADES);
    }

    @Override
    public boolean configurada(DatosConexionIntegracion datos) {
        return datos != null && presente(datos.credencial("url")) && presente(datos.credencial("db"))
                && presente(datos.credencial("username"))
                && (presente(datos.credencial("password")) || presente(datos.credencial("apiKey")));
    }

    @Override
    public ResultadoIntegracion probarConexion(DatosConexionIntegracion datos) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Odoo sin credenciales mínimas.");
        }
        String base = normalizarUrl(datos.credencial("url"));
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Odoo inválida.");
        }
        Sesion sesion = autenticar(datos, base);
        if (sesion.error() != null) {
            return ResultadoIntegracion.fallo(sesion.error());
        }
        return ResultadoIntegracion.ok("Conexión con Odoo verificada.", sesion.uid());
    }

    @Override
    public ResultadoIntegracion sincronizarCliente(DatosConexionIntegracion datos, ClienteIntegracion cliente) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Odoo sin credenciales mínimas.");
        }
        if (cliente == null || !presente(cliente.razonSocial())) {
            return ResultadoIntegracion.fallo("Datos de cliente incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Odoo inválida.");
        }
        Sesion sesion = autenticar(datos, base);
        if (sesion.error() != null) {
            return ResultadoIntegracion.fallo(sesion.error());
        }
        ObjectNode valores = mapper.createObjectNode();
        valores.put("name", cliente.razonSocial());
        valores.put("company_type", "company");
        poner(valores, "vat", cliente.nit());
        poner(valores, "email", cliente.email());
        poner(valores, "phone", cliente.telefono());
        poner(valores, "street", cliente.direccion());
        JsonNode id = llamar(base, "res.partner", "create", valores, sesion, datos);
        if (id == null || !presente(id.asText())) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Odoo.");
        }
        return ResultadoIntegracion.ok("Cliente sincronizado en Odoo.", id.asText());
    }

    @Override
    public ResultadoIntegracion sincronizarProducto(DatosConexionIntegracion datos, ProductoIntegracion producto) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Odoo sin credenciales mínimas.");
        }
        if (producto == null || !presente(producto.nombre()) || !presente(producto.sku())
                || producto.precio() == null) {
            return ResultadoIntegracion.fallo("Datos de producto incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Odoo inválida.");
        }
        Sesion sesion = autenticar(datos, base);
        if (sesion.error() != null) {
            return ResultadoIntegracion.fallo(sesion.error());
        }
        ObjectNode valores = mapper.createObjectNode();
        valores.put("name", producto.nombre());
        valores.put("default_code", producto.sku());
        valores.put("list_price", producto.precio());
        valores.put("sale_ok", true);
        valores.put("purchase_ok", false);
        poner(valores, "description", producto.impuesto());
        JsonNode id = llamar(base, "product.product", "create", valores, sesion, datos);
        if (id == null || !presente(id.asText())) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Odoo.");
        }
        return ResultadoIntegracion.ok("Producto sincronizado en Odoo.", id.asText());
    }

    @Override
    public ResultadoIntegracion enviarFactura(DatosConexionIntegracion datos, FacturaIntegracion factura) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Odoo sin credenciales mínimas.");
        }
        if (factura == null || !presente(factura.numero())
                || (!presente(factura.clienteId()) && !presente(factura.clienteNit()))
                || factura.total() == null) {
            return ResultadoIntegracion.fallo("Datos de factura incompletos.");
        }
        String base = base(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Odoo inválida.");
        }
        Sesion sesion = autenticar(datos, base);
        if (sesion.error() != null) {
            return ResultadoIntegracion.fallo(sesion.error());
        }
        ObjectNode valores = mapper.createObjectNode();
        valores.put("move_type", "out_invoice");
        valores.put("name", factura.numero());
        if (factura.fecha() != null) {
            valores.put("invoice_date", factura.fecha().toString());
        }
        if (presente(factura.clienteId()) && factura.clienteId().matches("\\d+")) {
            valores.put("partner_id", Integer.parseInt(factura.clienteId()));
        }
        poner(valores, "partner_ref", factura.clienteNit());
        valores.put("currency_id", idMoneda(factura.moneda()));
        valores.put("amount_total", factura.total());
        ArrayNode lineas = valores.putArray("invoice_line_ids");
        if (factura.lineas() != null) {
            for (LineaFacturaIntegracion linea : factura.lineas()) {
                ArrayNode comando = lineas.addArray();
                comando.add(0);
                comando.add(0);
                ObjectNode valorLinea = comando.addObject();
                poner(valorLinea, "name", linea.descripcion());
                if (presente(linea.productoId()) && linea.productoId().matches("\\d+")) {
                    valorLinea.put("product_id", Integer.parseInt(linea.productoId()));
                }
                valorLinea.put("quantity", linea.cantidad() == null ? BigDecimal.ONE : linea.cantidad());
                valorLinea.put("price_unit", linea.valorUnitario() == null ? BigDecimal.ZERO : linea.valorUnitario());
                if (linea.impuesto() != null && linea.impuesto().compareTo(BigDecimal.ZERO) != 0) {
                    valorLinea.put("price_tax", linea.impuesto());
                }
            }
        }
        JsonNode id = llamar(base, "account.move", "create", valores, sesion, datos);
        if (id == null || !presente(id.asText())) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Odoo.");
        }
        return ResultadoIntegracion.ok("Factura emitida en Odoo.", id.asText());
    }

    @Override
    public ResultadoIntegracion procesarWebhook(DatosConexionIntegracion datos, EventoIntegracion evento) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Odoo sin credenciales mínimas.");
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
        String id = primeraTexto(raiz, "id", "idExterno", "res_id", "record_id");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Webhook sin identificador.");
        }
        String entidad;
        if (raiz.has("amount_total") || raiz.has("invoice_line_ids") || raiz.has("move_type")) {
            entidad = "FACTURA";
        } else if (raiz.has("vat") || raiz.has("email") || raiz.has("partner_id")) {
            entidad = "CLIENTE";
        } else if (raiz.has("default_code") || raiz.has("list_price") || raiz.has("product_id")) {
            entidad = "PRODUCTO";
        } else {
            entidad = "EVENTO";
        }
        return new ResultadoIntegracion(true, "Webhook de " + entidad.toLowerCase(Locale.ROOT)
                + " procesado.", id, "EXITOSO", Map.of("entidad", entidad));
    }

    private Sesion autenticar(DatosConexionIntegracion datos, String base) {
        ObjectNode parametros = mapper.createObjectNode();
        parametros.put("db", datos.credencial("db"));
        parametros.put("login", datos.credencial("username"));
        parametros.put("password", clave(datos));
        RespuestaHttp respuesta = ejecutar(base + "/web/session/authenticate", "POST",
                jsonRpc(parametros), null, datos);
        if (respuesta.error() != null) {
            return new Sesion(null, null, respuesta.error());
        }
        JsonNode resultado = resultado(respuesta);
        JsonNode uid = resultado == null ? null : resultado.get("uid");
        if (uid == null || uid.isNull() || !uid.isNumber() || uid.asLong() <= 0) {
            return new Sesion(null, null, "Respuesta inválida de Odoo.");
        }
        return new Sesion(uid.asText(), cookieSesion(respuesta.cabeceras()), null);
    }

    private JsonNode llamar(String base, String modelo, String metodo, ObjectNode valores,
                            Sesion sesion, DatosConexionIntegracion datos) {
        ObjectNode parametros = mapper.createObjectNode();
        parametros.put("model", modelo);
        parametros.put("method", metodo);
        ArrayNode argumentos = parametros.putArray("args");
        argumentos.addArray().add(valores);
        parametros.putObject("kwargs");
        RespuestaHttp respuesta = ejecutar(base + "/web/dataset/call_kw", "POST",
                jsonRpc(parametros), sesion.cookie(), datos);
        return respuesta.error() == null ? resultado(respuesta) : null;
    }

    private String jsonRpc(ObjectNode parametros) {
        ObjectNode raiz = mapper.createObjectNode();
        raiz.put("jsonrpc", "2.0");
        raiz.put("method", "call");
        raiz.set("params", parametros);
        raiz.put("id", 1);
        return raiz.toString();
    }

    private JsonNode resultado(RespuestaHttp respuesta) {
        JsonNode cuerpo = respuesta.cuerpo();
        if (cuerpo == null) {
            return null;
        }
        JsonNode error = cuerpo.get("error");
        if (error != null && !error.isNull()) {
            return null;
        }
        JsonNode resultado = cuerpo.get("result");
        return resultado == null || resultado.isNull() ? null : resultado;
    }

    private RespuestaHttp ejecutar(String url, String metodo, String cuerpo, String cookie,
                                   DatosConexionIntegracion datos) {
        String contenido;
        HttpHeaders cabeceras;
        try {
            HttpClient cliente = fabricaHttp.apply(Duration.ofMillis(timeout(datos, "connectTimeoutMs", "5000")));
            HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeout(datos, "readTimeoutMs", "10000")))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json");
            if (presente(cookie)) {
                peticion.header("Cookie", "session_id=" + cookie);
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
                return new RespuestaHttp(null, null, "Odoo devolvió un error HTTP.");
            }
            contenido = respuesta.body();
            cabeceras = respuesta.headers();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new RespuestaHttp(null, null, "Operación interrumpida.");
        } catch (IOException | RuntimeException ex) {
            return new RespuestaHttp(null, null, "No se pudo comunicar con Odoo.");
        }
        try {
            return new RespuestaHttp(mapper.readTree(contenido), cabeceras, null);
        } catch (Exception ex) {
            return new RespuestaHttp(null, cabeceras, "Respuesta inválida de Odoo.");
        }
    }

    private static String base(DatosConexionIntegracion datos) {
        return normalizarUrl(datos.credencial("url"));
    }

    private static String clave(DatosConexionIntegracion datos) {
        return presente(datos.credencial("password")) ? datos.credencial("password") : datos.credencial("apiKey");
    }

    private static String cookieSesion(HttpHeaders cabeceras) {
        if (cabeceras == null) {
            return null;
        }
        String valor = cabeceras.firstValue("Set-Cookie").orElse(null);
        if (!presente(valor)) {
            return null;
        }
        int inicio = valor.indexOf("session_id=");
        if (inicio < 0) {
            return null;
        }
        inicio += "session_id=".length();
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

    private static int idMoneda(String moneda) {
        if ("USD".equalsIgnoreCase(moneda)) {
            return 2;
        }
        if ("EUR".equalsIgnoreCase(moneda)) {
            return 3;
        }
        return 1;
    }

    private static void poner(ObjectNode nodo, String clave, String valor) {
        if (presente(valor)) {
            nodo.put(clave, valor);
        }
    }

    private static String primeraTexto(JsonNode nodo, String... campos) {
        for (String campo : campos) {
            JsonNode valor = nodo.get(campo);
            if (valor != null && !valor.isNull() && presente(valor.asText())) {
                return valor.asText();
            }
        }
        return null;
    }

    private static boolean presente(String valor) {
        return valor != null && !valor.isBlank();
    }

    private record Sesion(String uid, String cookie, String error) {
    }

    private record RespuestaHttp(JsonNode cuerpo, HttpHeaders cabeceras, String error) {
    }
}
