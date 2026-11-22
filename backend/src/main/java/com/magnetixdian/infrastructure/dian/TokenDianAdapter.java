package com.magnetixdian.infrastructure.dian;

import com.magnetixdian.domain.dian.ArchivoListadoDian;
import com.magnetixdian.domain.dian.OrigenListadoDian;
import com.magnetixdian.domain.dian.ProveedorListadosDian;
import com.magnetixdian.domain.dian.SolicitudListadoDian;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TokenDianAdapter implements ProveedorListadosDian {

    private static final String ACEPTAR = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final Pattern ENLACE = Pattern.compile("href\\s*=\\s*[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NOMBRE_ARCHIVO = Pattern.compile("filename\\s*=\\s*\"?([^\";]+)\"?",
            Pattern.CASE_INSENSITIVE);
    private static final List<String> EXTENSIONES = List.of(".xlsx", ".xls", ".csv", ".zip", ".json");

    private final URI baseUrl;
    private final String tokenUrl;
    private final String listadoUrl;
    private final List<String> hostsPermitidos;
    private final Duration readTimeout;
    private final long maxBytes;
    private final FabricaHttpClientDian fabrica;

    @Autowired
    public TokenDianAdapter(
            @Value("${magnetixdian.dian.catalogo.base-url:https://catalogo-vpfe.dian.gov.co}") String baseUrl,
            @Value("${magnetixdian.dian.catalogo.token-url:}") String tokenUrl,
            @Value("${magnetixdian.dian.catalogo.listado-url:}") String listadoUrl,
            @Value("${magnetixdian.dian.catalogo.hosts-permitidos:catalogo-vpfe.dian.gov.co,vpfe.dian.gov.co,vpfe-hab.dian.gov.co}") String hostsPermitidos,
            @Value("${magnetixdian.dian.catalogo.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${magnetixdian.dian.catalogo.read-timeout-ms:60000}") long readTimeoutMs,
            @Value("${magnetixdian.dian.catalogo.max-bytes:52428800}") long maxBytes) {
        this(baseUrl, tokenUrl, listadoUrl, hostsPermitidos, readTimeoutMs, maxBytes,
                fabricaPorDefecto(connectTimeoutMs));
    }

    TokenDianAdapter(String baseUrl, String tokenUrl, String listadoUrl, String hostsPermitidos,
                     long readTimeoutMs, long maxBytes, FabricaHttpClientDian fabrica) {
        String base = baseUrl == null || baseUrl.isBlank() ? "https://catalogo-vpfe.dian.gov.co" : baseUrl.trim();
        this.baseUrl = URI.create(base);
        this.tokenUrl = tokenUrl == null ? "" : tokenUrl.trim();
        this.listadoUrl = listadoUrl == null ? "" : listadoUrl.trim();
        this.hostsPermitidos = Arrays.stream((hostsPermitidos == null ? "" : hostsPermitidos).split(","))
                .map(String::trim)
                .map(host -> host.toLowerCase(Locale.ROOT))
                .filter(host -> !host.isEmpty())
                .toList();
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
        this.maxBytes = maxBytes;
        this.fabrica = fabrica;
    }

    @Override
    public OrigenListadoDian origen() {
        return OrigenListadoDian.TOKEN;
    }

    @Override
    public boolean soporta(SolicitudListadoDian solicitud) {
        return solicitud != null && solicitud.token() != null && !solicitud.token().isBlank();
    }

    @Override
    public ArchivoListadoDian obtener(SolicitudListadoDian solicitud) {
        if (!soporta(solicitud)) {
            throw new IllegalArgumentException("El token de la DIAN es obligatorio.");
        }
        HttpClient cliente = fabrica.crear();
        URI inicio = resolverEnlaceInicial(solicitud.token().trim());
        RespuestaHttp respuestaInicial = enviar(cliente, HttpRequest.newBuilder(inicio)
                .GET()
                .timeout(readTimeout)
                .header("Accept", ACEPTAR)
                .build());
        URI listado = resolverListado();
        RespuestaHttp respuestaListado = enviar(cliente, HttpRequest.newBuilder(listado)
                .POST(HttpRequest.BodyPublishers.ofString(formulario(solicitud)))
                .timeout(readTimeout)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", ACEPTAR)
                .header("Referer", respuestaInicial.uri().toString())
                .build());
        return convertirDescarga(cliente, listado, respuestaListado);
    }

    private static FabricaHttpClientDian fabricaPorDefecto(long connectTimeoutMs) {
        return () -> HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
    }

    private URI resolverEnlaceInicial(String token) {
        URI enlace = intentarEnlace(token);
        if (enlace != null) {
            return validar(enlace);
        }
        if (tokenUrl.isEmpty()) {
            throw new IllegalArgumentException("El token DIAN debe ser el enlace completo o configurar DIAN_CATALOGO_TOKEN_URL.");
        }
        String codificado = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String destino = tokenUrl.contains("{token}")
                ? tokenUrl.replace("{token}", codificado)
                : tokenUrl + (tokenUrl.contains("?") ? "&token=" : "?token=") + codificado;
        return validar(crearUri(destino));
    }

    private static URI intentarEnlace(String valor) {
        try {
            URI uri = URI.create(valor);
            return uri.getScheme() == null ? null : uri;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private URI resolverListado() {
        if (listadoUrl.isEmpty()) {
            throw new IllegalArgumentException("No se configuró la descarga de listados DIAN (DIAN_CATALOGO_LISTADO_URL).");
        }
        URI uri = crearUri(listadoUrl);
        return validar(uri.isAbsolute() ? uri : baseUrl.resolve(uri));
    }

    private URI validar(URI uri) {
        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(esquema) && !"https".equals(esquema)) {
            throw new IllegalArgumentException("El enlace DIAN debe usar HTTP o HTTPS.");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean permitido = hostsPermitidos.stream()
                .anyMatch(dominio -> host.equals(dominio) || host.endsWith("." + dominio));
        if (!permitido) {
            throw new IllegalArgumentException("El enlace DIAN no está permitido.");
        }
        return uri;
    }

    private static URI crearUri(String valor) {
        try {
            return URI.create(valor);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("La configuración de la DIAN no es una URL válida.", ex);
        }
    }

    private String formulario(SolicitudListadoDian solicitud) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("fechaDesde", solicitud.fechaDesde().toString());
        parametros.put("fechaHasta", solicitud.fechaHasta().toString());
        parametros.put("fechaInicial", solicitud.fechaDesde().toString());
        parametros.put("fechaFinal", solicitud.fechaHasta().toString());
        agregar(parametros, "grupo", solicitud.grupo());
        agregar(parametros, "nitTercero", solicitud.nitTercero());
        StringBuilder cuerpo = new StringBuilder();
        parametros.forEach((clave, valor) -> {
            if (cuerpo.length() > 0) {
                cuerpo.append('&');
            }
            cuerpo.append(clave).append('=').append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
        });
        return cuerpo.toString();
    }

    private static void agregar(Map<String, String> parametros, String clave, String valor) {
        if (valor != null && !valor.isBlank()) {
            parametros.put(clave, valor.trim());
        }
    }

    private RespuestaHttp enviar(HttpClient cliente, HttpRequest solicitud) {
        try {
            HttpResponse<byte[]> respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofByteArray());
            if (respuesta.body().length > maxBytes) {
                throw new IllegalArgumentException("La DIAN devolvió un listado demasiado grande.");
            }
            int estado = respuesta.statusCode();
            if (estado < 200 || estado >= 300) {
                throw new IllegalArgumentException("La DIAN rechazó la consulta (HTTP " + estado + "). Verifique el token o enlace.");
            }
            return new RespuestaHttp(estado, respuesta.headers(), respuesta.body(), respuesta.uri());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta a la DIAN interrumpida.", ex);
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo consultar la DIAN.", ex);
        }
    }

    private ArchivoListadoDian convertirDescarga(HttpClient cliente, URI listado, RespuestaHttp respuesta) {
        String nombre = nombreArchivo(respuesta.cabeceras(), "listado-dian.xlsx");
        if (esArchivo(respuesta, nombre)) {
            return new ArchivoListadoDian(nombre, respuesta.cuerpo());
        }
        String html = new String(respuesta.cuerpo(), StandardCharsets.UTF_8);
        String enlace = encontrarEnlace(html);
        if (enlace == null) {
            throw new IllegalArgumentException("La DIAN no devolvió un listado descargable. Verifique los filtros.");
        }
        URI descarga = validar(listado.resolve(enlace));
        RespuestaHttp archivo = enviar(cliente, HttpRequest.newBuilder(descarga)
                .GET()
                .timeout(readTimeout)
                .header("Accept", ACEPTAR)
                .header("Referer", listado.toString())
                .build());
        return new ArchivoListadoDian(nombreArchivo(archivo.cabeceras(), nombre), archivo.cuerpo());
    }

    private static boolean esArchivo(RespuestaHttp respuesta, String nombre) {
        byte[] cuerpo = respuesta.cuerpo();
        if (cuerpo.length >= 2 && cuerpo[0] == 'P' && cuerpo[1] == 'K') {
            return true;
        }
        String tipo = respuesta.cabeceras().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        if (tipo.contains("spreadsheet") || tipo.contains("excel") || tipo.contains("zip")
                || tipo.contains("octet-stream") || tipo.contains("csv")) {
            return true;
        }
        String archivo = nombre.toLowerCase(Locale.ROOT);
        return EXTENSIONES.stream().anyMatch(archivo::endsWith);
    }

    private static String encontrarEnlace(String html) {
        Matcher matcher = ENLACE.matcher(html);
        String respaldo = null;
        while (matcher.find()) {
            String href = matcher.group(1).replace("&amp;", "&").trim();
            String normalizado = href.toLowerCase(Locale.ROOT);
            if (EXTENSIONES.stream().anyMatch(normalizado::contains) || normalizado.contains("descarg")) {
                return href;
            }
            if (respaldo == null && normalizado.contains("export")) {
                respaldo = href;
            }
        }
        return respaldo;
    }

    private static String nombreArchivo(HttpHeaders cabeceras, String defecto) {
        return cabeceras.firstValue("Content-Disposition")
                .flatMap(TokenDianAdapter::extraerNombre)
                .orElse(defecto);
    }

    private static Optional<String> extraerNombre(String cabecera) {
        Matcher matcher = NOMBRE_ARCHIVO.matcher(cabecera);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String nombre = matcher.group(1).trim();
        if (nombre.contains("''")) {
            nombre = nombre.substring(nombre.lastIndexOf("''") + 2);
        }
        nombre = nombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return nombre.isEmpty() ? Optional.empty() : Optional.of(nombre);
    }

    private record RespuestaHttp(int estado, HttpHeaders cabeceras, byte[] cuerpo, URI uri) {
    }
}
