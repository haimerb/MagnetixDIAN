package com.magnetixdian.infrastructure.integracion.helisa;

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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
public class AdaptadorHelisa implements AdaptadorIntegracion {

    private static final String BASE_API = "/api/v1";
    private static final Set<CapacidadIntegracion> CAPACIDADES = EnumSet.of(
            CapacidadIntegracion.SYNC_CLIENTES,
            CapacidadIntegracion.SYNC_PRODUCTOS,
            CapacidadIntegracion.EMITIR_FACTURA,
            CapacidadIntegracion.CONSULTAR_ESTADO,
            CapacidadIntegracion.RECIBIR_WEBHOOK);

    private final Function<Duration, HttpClient> fabricaHttp;
    private final ObjectMapper mapper;

    @Autowired
    public AdaptadorHelisa() {
        this(duracion -> HttpClient.newBuilder().connectTimeout(duracion).build(), new ObjectMapper());
    }

    public AdaptadorHelisa(Function<Duration, HttpClient> fabricaHttp, ObjectMapper mapper) {
        this.fabricaHttp = fabricaHttp;
        this.mapper = mapper;
    }

    @Override
    public ProveedorIntegracion proveedor() {
        return ProveedorIntegracion.HELISA;
    }

    @Override
    public Set<CapacidadIntegracion> capacidades() {
        return Set.copyOf(CAPACIDADES);
    }

    @Override
    public boolean configurada(DatosConexionIntegracion datos) {
        return datos != null && presente(datos.credencial("usuario"))
                && presente(datos.credencial("clave"))
                && presente(datos.credencial("url"))
                && presente(datos.credencial("sign"))
                && presente(datos.credencial("empresa"));
    }

    @Override
    public ResultadoIntegracion probarConexion(DatosConexionIntegracion datos) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Helisa sin credenciales mínimas.");
        }
        String base = urlBase(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Helisa inválida.");
        }
        String ruta = BASE_API + "/empresas/" + codificar(datos.credencial("empresa"));
        RespuestaHttp respuesta = ejecutar(conSign(base, ruta, datos), "GET", null, datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "id");
        if (!presente(id)) {
            id = texto(respuesta.cuerpo(), "identificacion");
        }
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Helisa.");
        }
        return ResultadoIntegracion.ok("Conexión con Helisa verificada.", id);
    }

    @Override
    public ResultadoIntegracion sincronizarCliente(DatosConexionIntegracion datos, ClienteIntegracion cliente) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Helisa sin credenciales mínimas.");
        }
        String base = urlBase(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Helisa inválida.");
        }
        if (cliente == null || !presente(cliente.razonSocial())) {
            return ResultadoIntegracion.fallo("Datos de cliente incompletos.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        poner(nodo, "empresa", datos.credencial("empresa"));
        poner(nodo, "id", cliente.idExterno());
        poner(nodo, "identificacion", cliente.nit());
        nodo.put("razonSocial", cliente.razonSocial());
        poner(nodo, "email", cliente.email());
        poner(nodo, "telefono", cliente.telefono());
        poner(nodo, "direccion", cliente.direccion());
        RespuestaHttp respuesta = ejecutar(conSign(base, BASE_API + "/clientes", datos), "POST",
                nodo.toString(), datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "id");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Helisa.");
        }
        return ResultadoIntegracion.ok("Cliente sincronizado en Helisa.", id);
    }

    @Override
    public ResultadoIntegracion sincronizarProducto(DatosConexionIntegracion datos, ProductoIntegracion producto) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Helisa sin credenciales mínimas.");
        }
        String base = urlBase(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Helisa inválida.");
        }
        if (producto == null || !presente(producto.sku()) || !presente(producto.nombre())
                || producto.precio() == null) {
            return ResultadoIntegracion.fallo("Datos de producto incompletos.");
        }
        ObjectNode nodo = mapper.createObjectNode();
        poner(nodo, "empresa", datos.credencial("empresa"));
        poner(nodo, "id", producto.idExterno());
        nodo.put("codigo", producto.sku());
        nodo.put("nombre", producto.nombre());
        nodo.put("precio", producto.precio().toPlainString());
        poner(nodo, "impuesto", producto.impuesto());
        RespuestaHttp respuesta = ejecutar(conSign(base, BASE_API + "/productos", datos), "POST",
                nodo.toString(), datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "id");
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Helisa.");
        }
        return ResultadoIntegracion.ok("Producto sincronizado en Helisa.", id);
    }

    @Override
    public ResultadoIntegracion enviarFactura(DatosConexionIntegracion datos, FacturaIntegracion factura) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Helisa sin credenciales mínimas.");
        }
        String base = urlBase(datos);
        if (base == null) {
            return ResultadoIntegracion.fallo("URL de Helisa inválida.");
        }
        if (factura == null || !presente(factura.clienteNit()) || factura.total() == null) {
            return ResultadoIntegracion.fallo("Datos de factura incompletos.");
        }
        RespuestaHttp respuesta = ejecutar(conSign(base, BASE_API + "/facturas", datos), "POST",
                payloadFactura(factura, datos).toString(), datos);
        if (respuesta.error() != null) {
            return ResultadoIntegracion.fallo(respuesta.error());
        }
        String id = texto(respuesta.cuerpo(), "id");
        if (!presente(id)) {
            id = texto(respuesta.cuerpo(), "consecutivo");
        }
        if (!presente(id)) {
            return ResultadoIntegracion.fallo("Respuesta inválida de Helisa.");
        }
        return ResultadoIntegracion.ok("Factura emitida en Helisa.", id);
    }

    @Override
    public ResultadoIntegracion procesarWebhook(DatosConexionIntegracion datos, EventoIntegracion evento) {
        if (!configurada(datos)) {
            return ResultadoIntegracion.fallo("Helisa sin credenciales mínimas.");
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
        if (raiz.has("items") || raiz.has("totals") || raiz.has("factura")) {
            entidad = "FACTURA";
        } else if (raiz.has("identificacion") || raiz.has("email")) {
            entidad = "CLIENTE";
        } else if (raiz.has("codigo") || raiz.has("precio")) {
            entidad = "PRODUCTO";
        } else {
            entidad = "EVENTO";
        }
        return new ResultadoIntegracion(true, "Webhook de " + entidad.toLowerCase(Locale.ROOT)
                + " procesado.", id, "EXITOSO", Map.of("entidad", entidad));
    }

    @Override
    public boolean verificarFirma(DatosConexionIntegracion datos, EventoIntegracion evento) {
        String secreto = datos == null ? null : datos.credencial("webhookSecret");
        if (!presente(secreto) || evento == null) {
            return false;
        }
        String firma = cabecera(evento, "X-Helisa-Sign");
        if (!presente(firma)) {
            return AdaptadorIntegracion.super.verificarFirma(datos, evento);
        }
        String timestamp = cabecera(evento, "X-Helisa-Timestamp");
        if (!presente(timestamp) || !timestampValido(timestamp)) {
            return false;
        }
        String contenido = timestamp + "." + (evento.payload() == null ? "" : evento.payload());
        return MessageDigest.isEqual(hmacHex(secreto, contenido).getBytes(StandardCharsets.UTF_8),
                firma.trim().getBytes(StandardCharsets.UTF_8));
    }

    private ObjectNode payloadFactura(FacturaIntegracion factura, DatosConexionIntegracion datos) {
        ObjectNode nodo = mapper.createObjectNode();
        poner(nodo, "empresa", datos.credencial("empresa"));
        ObjectNode tercero = nodo.putObject("tercero");
        poner(tercero, "id", factura.clienteId());
        poner(tercero, "identificacion", factura.clienteNit());
        poner(nodo, "numero", factura.numero());
        if (factura.fecha() != null) {
            nodo.put("fecha", factura.fecha().toString());
        }
        poner(nodo, "moneda", factura.moneda());
        ArrayNode items = nodo.putArray("items");
        if (factura.lineas() != null) {
            for (LineaFacturaIntegracion linea : factura.lineas()) {
                ObjectNode item = items.addObject();
                poner(item, "productoId", linea.productoId());
                poner(item, "descripcion", linea.descripcion());
                item.put("cantidad", importe(linea.cantidad(), "1"));
                item.put("valorUnitario", importe(linea.valorUnitario(), "0"));
                item.put("impuesto", importe(linea.impuesto(), "0"));
            }
        }
        ObjectNode totales = nodo.putObject("totals");
        totales.put("subtotal", importe(factura.subtotal(), "0"));
        totales.put("impuestos", importe(factura.impuestos(), "0"));
        totales.put("total", importe(factura.total(), "0"));
        return nodo;
    }

    private RespuestaHttp ejecutar(String url, String metodo, String cuerpo, DatosConexionIntegracion datos) {
        String contenido;
        try {
            HttpClient cliente = fabricaHttp.apply(Duration.ofMillis(timeout(datos, "connectTimeoutMs", "5000")));
            HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeout(datos, "readTimeoutMs", "10000")))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + autorizacion(datos))
                    .header("X-Helisa-Sign", datos.credencial("sign"));
            if ("POST".equals(metodo)) {
                peticion.POST(HttpRequest.BodyPublishers.ofString(cuerpo == null ? "{}" : cuerpo,
                        StandardCharsets.UTF_8));
            } else {
                peticion.GET();
            }
            HttpResponse<String> respuesta = cliente.send(peticion.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                return new RespuestaHttp(null, "Helisa devolvió un error HTTP.");
            }
            contenido = respuesta.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new RespuestaHttp(null, "Operación interrumpida.");
        } catch (IOException | RuntimeException ex) {
            return new RespuestaHttp(null, "No se pudo comunicar con Helisa.");
        }
        try {
            return new RespuestaHttp(mapper.readTree(contenido), null);
        } catch (Exception ex) {
            return new RespuestaHttp(null, "Respuesta inválida de Helisa.");
        }
    }

    private static String autorizacion(DatosConexionIntegracion datos) {
        String par = datos.credencial("usuario") + ":" + datos.credencial("clave");
        return Base64.getEncoder().encodeToString(par.getBytes(StandardCharsets.UTF_8));
    }

    private static String conSign(String base, String ruta, DatosConexionIntegracion datos) {
        return base + ruta + "?sign=" + codificar(datos.credencial("sign"));
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
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

    private static boolean timestampValido(String timestamp) {
        try {
            return Math.abs(Instant.now().getEpochSecond() - Long.parseLong(timestamp)) <= 300;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String hmacHex(String secreto, String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular la firma del webhook", ex);
        }
    }

    private static String urlBase(DatosConexionIntegracion datos) {
        return normalizarUrl(datos.credencial("url"));
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
