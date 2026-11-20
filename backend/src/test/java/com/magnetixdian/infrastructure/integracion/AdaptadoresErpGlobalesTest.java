package com.magnetixdian.infrastructure.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.magnetixdian.infrastructure.integracion.dynamics.AdaptadorDynamics365;
import com.magnetixdian.infrastructure.integracion.netsuite.AdaptadorNetSuite;
import com.magnetixdian.infrastructure.integracion.odoo.AdaptadorOdoo;
import com.magnetixdian.infrastructure.integracion.sap.AdaptadorSapBusinessOne;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdaptadoresErpGlobalesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SECRETO = "secreto-webhook";

    @Test
    void credencialesAusentesDesactivanAdaptadoresSinRed() {
        HttpClient http = mock(HttpClient.class);
        List<AdaptadorIntegracion> adaptadores = adaptadores(http);
        for (AdaptadorIntegracion adaptador : adaptadores) {
            DatosConexionIntegracion sinCredenciales = datos(adaptador.proveedor(), Map.of(), Map.of());
            assertFalse(adaptador.configurada(sinCredenciales));
            assertFalse(adaptador.probarConexion(sinCredenciales).exitoso());
            assertTrue(adaptador.capacidades().containsAll(List.of(
                    CapacidadIntegracion.SYNC_CLIENTES,
                    CapacidadIntegracion.SYNC_PRODUCTOS,
                    CapacidadIntegracion.EMITIR_FACTURA,
                    CapacidadIntegracion.CONSULTAR_ESTADO,
                    CapacidadIntegracion.RECIBIR_WEBHOOK)));
        }
        assertFalse(new AdaptadorOdoo(duracion -> http, MAPPER).configurada(datos(ProveedorIntegracion.ODOO,
                Map.of("url", "https://odoo.example", "db", "demo", "username", "admin"), Map.of())));
        assertFalse(new AdaptadorSapBusinessOne(duracion -> http, MAPPER).configurada(
                datos(ProveedorIntegracion.SAP_BUSINESS_ONE,
                        Map.of("url", "https://sap.example", "companyDB", "demo", "username", "admin"), Map.of())));
        assertFalse(new AdaptadorDynamics365(duracion -> http, MAPPER).configurada(
                datos(ProveedorIntegracion.DYNAMICS_365,
                        Map.of("url", "https://org.crm.dynamics.com", "tenantId", "tenant", "clientId", "client"),
                        Map.of())));
        assertFalse(new AdaptadorNetSuite(duracion -> http, MAPPER).configurada(
                datos(ProveedorIntegracion.NETSUITE,
                        Map.of("accountId", "123456", "consumerKey", "ck", "consumerSecret", "cs",
                                "tokenId", "ti"), Map.of())));
        verifyNoInteractions(http);
    }

    @Test
    void probarConexionUsaTimeoutsYAutenticacionPorProveedor() throws Exception {
        HttpClient httpOdoo = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"result\":{\"uid\":7}}", cabeceras("Set-Cookie", "session_id=abc; Path=/")))
                .when(httpOdoo).send(any(), any());
        List<Duration> conexionOdoo = new ArrayList<>();
        ResultadoIntegracion odoo = new AdaptadorOdoo(duracion -> {
            conexionOdoo.add(duracion);
            return httpOdoo;
        }, MAPPER).probarConexion(datos(ProveedorIntegracion.ODOO,
                credenciales(ProveedorIntegracion.ODOO), Map.of("connectTimeoutMs", "750", "readTimeoutMs", "1250")));
        assertTrue(odoo.exitoso());
        assertEquals("7", odoo.idExterno());
        assertEquals(List.of(Duration.ofMillis(750)), conexionOdoo);
        HttpRequest solicitudOdoo = ultimaSolicitud(httpOdoo, 1);
        assertTrue(solicitudOdoo.uri().toString().endsWith("/web/session/authenticate"));
        assertEquals(Duration.ofMillis(1250), solicitudOdoo.timeout().orElseThrow());
        assertTrue(cuerpo(solicitudOdoo).contains("\"login\":\"admin\""));

        HttpClient httpSap = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"SessionId\":\"sesion-secreta\",\"CompanyName\":\"Demo SAP\"}",
                cabeceras("Set-Cookie", "ROUTEID=.node1"))).when(httpSap).send(any(), any());
        List<Duration> conexionSap = new ArrayList<>();
        ResultadoIntegracion sap = new AdaptadorSapBusinessOne(duracion -> {
            conexionSap.add(duracion);
            return httpSap;
        }, MAPPER).probarConexion(datos(ProveedorIntegracion.SAP_BUSINESS_ONE,
                credenciales(ProveedorIntegracion.SAP_BUSINESS_ONE),
                Map.of("connectTimeoutMs", "750", "readTimeoutMs", "1250")));
        assertTrue(sap.exitoso());
        assertEquals("Demo SAP", sap.idExterno());
        assertFalse(sap.idExterno().contains("sesion-secreta"));
        assertEquals(List.of(Duration.ofMillis(750)), conexionSap);
        HttpRequest solicitudSap = ultimaSolicitud(httpSap, 1);
        assertTrue(solicitudSap.uri().toString().endsWith("/b1s/v1/Login"));
        assertEquals(Duration.ofMillis(1250), solicitudSap.timeout().orElseThrow());
        assertTrue(cuerpo(solicitudSap).contains("\"CompanyDB\":\"demo\""));

        HttpClient httpDynamics = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"access_token\":\"token-dynamics\",\"expires_in\":3600}"),
                respuesta(200, "{\"UserId\":\"user-1\"}")).when(httpDynamics).send(any(), any());
        List<Duration> conexionDynamics = new ArrayList<>();
        ResultadoIntegracion dynamics = new AdaptadorDynamics365(duracion -> {
            conexionDynamics.add(duracion);
            return httpDynamics;
        }, MAPPER).probarConexion(datos(ProveedorIntegracion.DYNAMICS_365,
                credenciales(ProveedorIntegracion.DYNAMICS_365),
                Map.of("connectTimeoutMs", "750", "readTimeoutMs", "1250")));
        assertTrue(dynamics.exitoso());
        assertEquals("user-1", dynamics.idExterno());
        assertTrue(conexionDynamics.stream().allMatch(duracion -> duracion.equals(Duration.ofMillis(750))));
        List<HttpRequest> solicitudesDynamics = solicitudes(httpDynamics, 2);
        assertTrue(solicitudesDynamics.get(0).uri().toString().contains("/tenant-demo/oauth2/v2.0/token"));
        String cuerpoToken = cuerpo(solicitudesDynamics.get(0));
        assertTrue(cuerpoToken.contains("grant_type=client_credentials"));
        assertTrue(cuerpoToken.contains("scope=https%3A%2F%2Forg.crm.dynamics.com%2F.default"));
        assertTrue(solicitudesDynamics.get(1).uri().toString().endsWith("/api/data/v9.2/WhoAmI"));
        assertEquals("Bearer token-dynamics",
                solicitudesDynamics.get(1).headers().firstValue("Authorization").orElseThrow());
        assertEquals(Duration.ofMillis(1250), solicitudesDynamics.get(1).timeout().orElseThrow());

        HttpClient httpNetSuite = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"items\":[]}")).when(httpNetSuite).send(any(), any());
        List<Duration> conexionNetSuite = new ArrayList<>();
        ResultadoIntegracion netSuite = new AdaptadorNetSuite(duracion -> {
            conexionNetSuite.add(duracion);
            return httpNetSuite;
        }, MAPPER).probarConexion(datos(ProveedorIntegracion.NETSUITE,
                credenciales(ProveedorIntegracion.NETSUITE),
                Map.of("connectTimeoutMs", "750", "readTimeoutMs", "1250")));
        assertTrue(netSuite.exitoso());
        assertEquals("netsuite-123456", netSuite.idExterno());
        assertEquals(List.of(Duration.ofMillis(750)), conexionNetSuite);
        HttpRequest solicitudNetSuite = ultimaSolicitud(httpNetSuite, 1);
        assertEquals("https://123456.suitetalk.api.netsuite.com/services/rest/record/v1/customer?limit=1",
                solicitudNetSuite.uri().toString());
        assertEquals(Duration.ofMillis(1250), solicitudNetSuite.timeout().orElseThrow());
        String oauth = solicitudNetSuite.headers().firstValue("Authorization").orElseThrow();
        assertTrue(oauth.startsWith("OAuth realm=\"123456\""), oauth);
        assertTrue(oauth.contains("oauth_signature_method=\"HMAC-SHA256\""));
        assertTrue(oauth.contains("oauth_signature=\""));
    }

    @Test
    void mapeoDeFacturaIncluyeLineasYTotalesEnCadaErp() throws Exception {
        HttpClient httpOdoo = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"result\":{\"uid\":7}}", cabeceras("Set-Cookie", "session_id=abc; Path=/")),
                respuesta(200, "{\"result\":55}")).when(httpOdoo).send(any(), any());
        ResultadoIntegracion odoo = new AdaptadorOdoo(duracion -> httpOdoo, MAPPER)
                .enviarFactura(datos(ProveedorIntegracion.ODOO, credenciales(ProveedorIntegracion.ODOO), Map.of()),
                        factura());
        assertTrue(odoo.exitoso());
        assertEquals("55", odoo.idExterno());
        HttpRequest solicitudOdoo = ultimaSolicitud(httpOdoo, 2);
        assertTrue(solicitudOdoo.uri().toString().endsWith("/web/dataset/call_kw"));
        assertEquals("session_id=abc", solicitudOdoo.headers().firstValue("Cookie").orElseThrow());
        String cuerpoOdoo = cuerpo(solicitudOdoo);
        assertTrue(cuerpoOdoo.contains("\"model\":\"account.move\""));
        assertTrue(cuerpoOdoo.contains("\"name\":\"FE-001\""));
        assertTrue(cuerpoOdoo.contains("\"invoice_line_ids\""));
        assertTrue(cuerpoOdoo.contains("119000.00"));

        HttpClient httpSap = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"SessionId\":\"sesion-secreta\"}",
                cabeceras("Set-Cookie", "ROUTEID=.node1")),
                respuesta(201, "{\"DocEntry\":77}")).when(httpSap).send(any(), any());
        ResultadoIntegracion sap = new AdaptadorSapBusinessOne(duracion -> httpSap, MAPPER)
                .enviarFactura(datos(ProveedorIntegracion.SAP_BUSINESS_ONE,
                        credenciales(ProveedorIntegracion.SAP_BUSINESS_ONE), Map.of()), factura());
        assertTrue(sap.exitoso());
        assertEquals("77", sap.idExterno());
        HttpRequest solicitudSap = ultimaSolicitud(httpSap, 2);
        assertTrue(solicitudSap.uri().toString().endsWith("/b1s/v1/Invoices"));
        assertEquals("B1SESSION=sesion-secreta; ROUTEID=.node1",
                solicitudSap.headers().firstValue("Cookie").orElseThrow());
        String cuerpoSap = cuerpo(solicitudSap);
        assertTrue(cuerpoSap.contains("\"CardCode\":\"cli-1\""));
        assertTrue(cuerpoSap.contains("\"DocumentLines\""));
        assertTrue(cuerpoSap.contains("119000.00"));

        HttpClient httpDynamics = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"access_token\":\"token-dynamics\"}"),
                respuesta(201, "{\"invoiceid\":\"inv-1\"}")).when(httpDynamics).send(any(), any());
        ResultadoIntegracion dynamics = new AdaptadorDynamics365(duracion -> httpDynamics, MAPPER)
                .enviarFactura(datos(ProveedorIntegracion.DYNAMICS_365,
                        credenciales(ProveedorIntegracion.DYNAMICS_365), Map.of()), factura());
        assertTrue(dynamics.exitoso());
        assertEquals("inv-1", dynamics.idExterno());
        HttpRequest solicitudDynamics = ultimaSolicitud(httpDynamics, 2);
        assertTrue(solicitudDynamics.uri().toString().endsWith("/api/data/v9.2/invoices"));
        assertEquals("Bearer token-dynamics",
                solicitudDynamics.headers().firstValue("Authorization").orElseThrow());
        String cuerpoDynamics = cuerpo(solicitudDynamics);
        assertTrue(cuerpoDynamics.contains("\"name\":\"FE-001\""));
        assertTrue(cuerpoDynamics.contains("\"invoice_details\""));
        assertTrue(cuerpoDynamics.contains("119000.00"));

        HttpClient httpNetSuite = mock(HttpClient.class);
        doReturn(respuesta(201, "{\"id\":\"88\"}",
                cabeceras("Location", "/services/rest/record/v1/invoice/88"))).when(httpNetSuite).send(any(), any());
        ResultadoIntegracion netSuite = new AdaptadorNetSuite(duracion -> httpNetSuite, MAPPER)
                .enviarFactura(datos(ProveedorIntegracion.NETSUITE,
                        credenciales(ProveedorIntegracion.NETSUITE), Map.of()), factura());
        assertTrue(netSuite.exitoso());
        assertEquals("88", netSuite.idExterno());
        HttpRequest solicitudNetSuite = ultimaSolicitud(httpNetSuite, 1);
        assertTrue(solicitudNetSuite.uri().toString().endsWith("/services/rest/record/v1/invoice"));
        assertTrue(solicitudNetSuite.headers().firstValue("Authorization").orElseThrow().startsWith("OAuth "));
        String cuerpoNetSuite = cuerpo(solicitudNetSuite);
        assertTrue(cuerpoNetSuite.contains("\"tranId\":\"FE-001\""));
        assertTrue(cuerpoNetSuite.contains("\"items\""));
        assertTrue(cuerpoNetSuite.contains("119000.00"));
    }

    @Test
    void sincronizacionDeClienteYProductoGeneraPayloadsDelProveedor() throws Exception {
        List<Caso> casos = List.of(
                new Caso(ProveedorIntegracion.ODOO, "/web/dataset/call_kw", "/web/dataset/call_kw",
                        "{\"result\":11}", "{\"result\":12}", "\"res.partner\"", "\"product.product\""),
                new Caso(ProveedorIntegracion.SAP_BUSINESS_ONE, "/b1s/v1/BusinessPartners", "/b1s/v1/Items",
                        "{\"CardCode\":\"900123456\"}", "{\"ItemCode\":\"SKU-1\"}",
                        "\"CardName\":\"Demo SAS\"", "\"ItemCode\":\"SKU-1\""),
                new Caso(ProveedorIntegracion.DYNAMICS_365, "/api/data/v9.2/accounts", "/api/data/v9.2/items",
                        "{\"accountid\":\"account-1\"}", "{\"itemid\":\"item-1\"}",
                        "\"accountnumber\":\"900123456\"", "\"productnumber\":\"SKU-1\""),
                new Caso(ProveedorIntegracion.NETSUITE, "/services/rest/record/v1/customer",
                        "/services/rest/record/v1/item", "{\"id\":\"cust-1\"}", "{\"id\":\"item-1\"}",
                        "\"companyName\":\"Demo SAS\"", "\"itemid\":\"SKU-1\""));

        for (Caso caso : casos) {
            int peticiones = caso.proveedor() == ProveedorIntegracion.NETSUITE ? 1 : 2;
            HttpClient httpCliente = mock(HttpClient.class);
            if (peticiones == 1) {
                doReturn(respuesta(200, caso.respuestaCliente())).when(httpCliente).send(any(), any());
            } else {
                doReturn(respuesta(200, respuestaAutenticacion(caso.proveedor())),
                        respuesta(200, caso.respuestaCliente())).when(httpCliente).send(any(), any());
            }
            DatosConexionIntegracion datos = datos(caso.proveedor(), credenciales(caso.proveedor()), Map.of());
            ResultadoIntegracion cliente = conHttp(caso.proveedor(), httpCliente)
                    .sincronizarCliente(datos, new ClienteIntegracion(null, "900123456", "Demo SAS",
                            "demo@example.com", "3001", "Calle 1"));
            assertTrue(cliente.exitoso(), caso.proveedor().name());
            HttpRequest solicitudCliente = ultimaSolicitud(httpCliente, peticiones);
            assertTrue(solicitudCliente.uri().toString().endsWith(caso.uriCliente()));
            assertTrue(cuerpo(solicitudCliente).contains(caso.marcaCliente()), caso.proveedor().name());

            HttpClient httpProducto = mock(HttpClient.class);
            if (peticiones == 1) {
                doReturn(respuesta(200, caso.respuestaProducto())).when(httpProducto).send(any(), any());
            } else {
                doReturn(respuesta(200, respuestaAutenticacion(caso.proveedor())),
                        respuesta(200, caso.respuestaProducto())).when(httpProducto).send(any(), any());
            }
            ResultadoIntegracion producto = conHttp(caso.proveedor(), httpProducto)
                    .sincronizarProducto(datos, new ProductoIntegracion(null, "SKU-1", "Servicio DIAN",
                            new BigDecimal("45000"), "IVA_19"));
            assertTrue(producto.exitoso(), caso.proveedor().name());
            HttpRequest solicitudProducto = ultimaSolicitud(httpProducto, peticiones);
            assertTrue(solicitudProducto.uri().toString().endsWith(caso.uriProducto()));
            assertTrue(cuerpo(solicitudProducto).contains(caso.marcaProducto()), caso.proveedor().name());
        }
    }

    @Test
    void firmaInvalidaOSecretoAusenteSeRechaza() {
        String payload = "{\"id\":\"evt-1\",\"move_type\":\"out_invoice\"}";
        String marca = String.valueOf(Instant.now().getEpochSecond());
        EventoIntegracion invalido = new EventoIntegracion("e1", "WEBHOOK", payload, Map.of(
                "X-Integracion-Timestamp", marca,
                "X-Integracion-Firma", "firma-invalida"));
        EventoIntegracion valido = new EventoIntegracion("e2", "WEBHOOK", payload, Map.of(
                "X-Integracion-Timestamp", marca,
                "X-Integracion-Firma", firmaHex(SECRETO, marca + "." + payload)));
        EventoIntegracion sinCabeceras = new EventoIntegracion("e3", "WEBHOOK", payload, Map.of());

        for (AdaptadorIntegracion adaptador : adaptadores(mock(HttpClient.class))) {
            DatosConexionIntegracion datos = datos(adaptador.proveedor(),
                    credenciales(adaptador.proveedor()), Map.of());
            Map<String, String> sinSecreto = new HashMap<>(credenciales(adaptador.proveedor()));
            sinSecreto.remove("webhookSecret");
            DatosConexionIntegracion sinSecretoDatos = datos(adaptador.proveedor(), sinSecreto, Map.of());

            assertFalse(adaptador.verificarFirma(datos, invalido));
            ResultadoIntegracion rechazado = adaptador.procesarWebhook(datos, invalido);
            assertFalse(rechazado.exitoso());
            assertEquals("Firma de webhook inválida.", rechazado.mensaje());

            assertTrue(adaptador.verificarFirma(datos, valido), adaptador.proveedor().name());
            assertTrue(adaptador.procesarWebhook(datos, valido).exitoso());
            assertFalse(adaptador.verificarFirma(sinSecretoDatos, valido));
            assertFalse(adaptador.procesarWebhook(sinSecretoDatos, valido).exitoso());
            assertFalse(adaptador.verificarFirma(datos, sinCabeceras));
        }
    }

    private static List<AdaptadorIntegracion> adaptadores(HttpClient http) {
        return List.of(
                new AdaptadorOdoo(duracion -> http, MAPPER),
                new AdaptadorSapBusinessOne(duracion -> http, MAPPER),
                new AdaptadorDynamics365(duracion -> http, MAPPER),
                new AdaptadorNetSuite(duracion -> http, MAPPER));
    }

    private static AdaptadorIntegracion conHttp(ProveedorIntegracion proveedor, HttpClient http) {
        return switch (proveedor) {
            case ODOO -> new AdaptadorOdoo(duracion -> http, MAPPER);
            case SAP_BUSINESS_ONE -> new AdaptadorSapBusinessOne(duracion -> http, MAPPER);
            case DYNAMICS_365 -> new AdaptadorDynamics365(duracion -> http, MAPPER);
            case NETSUITE -> new AdaptadorNetSuite(duracion -> http, MAPPER);
            default -> throw new IllegalArgumentException(String.valueOf(proveedor));
        };
    }

    private static Map<String, String> credenciales(ProveedorIntegracion proveedor) {
        Map<String, String> base = new HashMap<>();
        base.put("webhookSecret", SECRETO);
        switch (proveedor) {
            case ODOO -> {
                base.put("url", "https://odoo.example");
                base.put("db", "demo");
                base.put("username", "admin");
                base.put("password", "clave-odoo");
            }
            case SAP_BUSINESS_ONE -> {
                base.put("url", "https://sap.example");
                base.put("companyDB", "demo");
                base.put("username", "admin");
                base.put("password", "clave-sap");
            }
            case DYNAMICS_365 -> {
                base.put("url", "https://org.crm.dynamics.com");
                base.put("tenantId", "tenant-demo");
                base.put("clientId", "client-demo");
                base.put("clientSecret", "clave-dynamics");
            }
            case NETSUITE -> {
                base.put("accountId", "123456");
                base.put("consumerKey", "ck-demo");
                base.put("consumerSecret", "cs-demo");
                base.put("tokenId", "ti-demo");
                base.put("tokenSecret", "ts-demo");
            }
            default -> throw new IllegalArgumentException(String.valueOf(proveedor));
        }
        return Map.copyOf(base);
    }

    private static DatosConexionIntegracion datos(ProveedorIntegracion proveedor,
                                                   Map<String, String> credenciales,
                                                   Map<String, String> opciones) {
        return new DatosConexionIntegracion(1L, proveedor, credenciales, opciones, true);
    }

    private static FacturaIntegracion factura() {
        return new FacturaIntegracion("fac-1", "FE-001", LocalDate.of(2026, 11, 20), "cli-1", "900123456",
                new BigDecimal("100000.00"), new BigDecimal("19000.00"), new BigDecimal("119000.00"), "COP",
                List.of(new LineaFacturaIntegracion("prod-1", "Servicio DIAN", new BigDecimal("2"),
                        new BigDecimal("50000.00"), new BigDecimal("19000.00"))),
                Map.of("origen", "TEST"));
    }

    private static String respuestaAutenticacion(ProveedorIntegracion proveedor) {
        return switch (proveedor) {
            case ODOO -> "{\"result\":{\"uid\":7}}";
            case SAP_BUSINESS_ONE -> "{\"SessionId\":\"sesion-secreta\"}";
            case DYNAMICS_365 -> "{\"access_token\":\"token-dynamics\"}";
            default -> throw new IllegalArgumentException(String.valueOf(proveedor));
        };
    }

    private static HttpResponse<String> respuesta(int estado, String cuerpo) {
        return respuesta(estado, cuerpo, HttpHeaders.of(Map.of(), (clave, contenido) -> true));
    }

    private static HttpResponse<String> respuesta(int estado, String cuerpo, HttpHeaders cabeceras) {
        HttpResponse<String> respuesta = mock(HttpResponse.class);
        when(respuesta.statusCode()).thenReturn(estado);
        when(respuesta.body()).thenReturn(cuerpo);
        when(respuesta.headers()).thenReturn(cabeceras);
        return respuesta;
    }

    private static HttpHeaders cabeceras(String nombre, String valor) {
        return HttpHeaders.of(Map.of(nombre, List.of(valor)), (clave, contenido) -> true);
    }

    private static HttpRequest ultimaSolicitud(HttpClient http, int cantidad) throws Exception {
        return solicitudes(http, cantidad).get(cantidad - 1);
    }

    private static List<HttpRequest> solicitudes(HttpClient http, int cantidad) throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(cantidad)).send(captor.capture(), any());
        return captor.getAllValues();
    }

    private static String cuerpo(HttpRequest solicitud) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        CompletableFuture<Void> fin = new CompletableFuture<>();
        solicitud.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Flow.Subscription suscripcion) {
                suscripcion.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer trozo) {
                byte[] bytes = new byte[trozo.remaining()];
                trozo.get(bytes);
                salida.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(Throwable error) {
                fin.completeExceptionally(error);
            }

            @Override
            public void onComplete() {
                fin.complete(null);
            }
        });
        fin.get(2, TimeUnit.SECONDS);
        return salida.toString(StandardCharsets.UTF_8);
    }

    private static String firmaHex(String secreto, String contenido) {
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
            throw new IllegalStateException(ex);
        }
    }

    private record Caso(ProveedorIntegracion proveedor, String uriCliente, String uriProducto,
                        String respuestaCliente, String respuestaProducto, String marcaCliente,
                        String marcaProducto) {
    }
}
