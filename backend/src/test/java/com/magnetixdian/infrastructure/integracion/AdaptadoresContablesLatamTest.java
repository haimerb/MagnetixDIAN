package com.magnetixdian.infrastructure.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.integracion.AdaptadorIntegracion;
import com.magnetixdian.domain.integracion.CapacidadIntegracion;
import com.magnetixdian.domain.integracion.DatosConexionIntegracion;
import com.magnetixdian.domain.integracion.EventoIntegracion;
import com.magnetixdian.domain.integracion.FacturaIntegracion;
import com.magnetixdian.domain.integracion.LineaFacturaIntegracion;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import com.magnetixdian.domain.integracion.ResultadoIntegracion;
import com.magnetixdian.infrastructure.integracion.alegra.AdaptadorAlegra;
import com.magnetixdian.infrastructure.integracion.helisa.AdaptadorHelisa;
import com.magnetixdian.infrastructure.integracion.siigo.AdaptadorSiigo;
import com.magnetixdian.infrastructure.integracion.worldoffice.AdaptadorWorldOffice;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.http.HttpClient;
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

class AdaptadoresContablesLatamTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SECRETO = "secreto-webhook";

    @Test
    void credencialesAusentesDesactivanAdaptadoresSinRed() {
        HttpClient http = mock(HttpClient.class);
        for (AdaptadorIntegracion adaptador : adaptadores(http)) {
            assertFalse(adaptador.configurada(datos(adaptador.proveedor(), Map.of(), Map.of())));
            assertFalse(adaptador.probarConexion(datos(adaptador.proveedor(), Map.of(), Map.of())).exitoso());
            assertTrue(adaptador.capacidades().containsAll(List.of(
                    CapacidadIntegracion.SYNC_CLIENTES,
                    CapacidadIntegracion.SYNC_PRODUCTOS,
                    CapacidadIntegracion.EMITIR_FACTURA,
                    CapacidadIntegracion.CONSULTAR_ESTADO,
                    CapacidadIntegracion.RECIBIR_WEBHOOK)));
        }
        assertFalse(new AdaptadorWorldOffice(duracion -> http, MAPPER)
                .configurada(datos(ProveedorIntegracion.WORLD_OFFICE,
                        Map.of("username", "demo"), Map.of())));
        assertFalse(new AdaptadorHelisa(duracion -> http, MAPPER)
                .configurada(datos(ProveedorIntegracion.HELISA,
                        Map.of("usuario", "demo", "clave", "clave", "url", "https://helisa.example"), Map.of())));
        verifyNoInteractions(http);
    }

    @Test
    void mapeoDeFacturaIncluyeItemsYTotalesEnCadaErp() throws Exception {
        HttpClient httpSiigo = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"access_token\":\"token-siigo\"}"),
                respuesta(201, "{\"id\":\"S-100\"}")).when(httpSiigo).send(any(), any());
        ResultadoIntegracion siigo = new AdaptadorSiigo(duracion -> httpSiigo, MAPPER)
                .enviarFactura(datos(ProveedorIntegracion.SIIGO, credenciales(ProveedorIntegracion.SIIGO), Map.of()),
                        factura());
        assertTrue(siigo.exitoso());
        assertEquals("S-100", siigo.idExterno());
        HttpRequest solicitudSiigo = ultimaSolicitud(httpSiigo, 2);
        assertTrue(solicitudSiigo.uri().toString().endsWith("/v1/invoices"));
        assertTrue(solicitudSiigo.headers().firstValue("Authorization").orElseThrow().startsWith("Bearer "));
        String cuerpoSiigo = cuerpo(solicitudSiigo);
        assertTrue(cuerpoSiigo.contains("\"items\""));
        assertTrue(cuerpoSiigo.contains("\"totals\""));
        assertTrue(cuerpoSiigo.contains("119000.00"));
        assertTrue(cuerpoSiigo.contains("\"quantity\":\"2\""));

        HttpClient httpAlegra = mock(HttpClient.class);
        doReturn(respuesta(201, "{\"id\":\"A-100\"}")).when(httpAlegra).send(any(), any());
        ResultadoIntegracion alegra = new AdaptadorAlegra(duracion -> httpAlegra, MAPPER)
                .enviarFactura(datos(ProveedorIntegracion.ALEGRA, credenciales(ProveedorIntegracion.ALEGRA), Map.of()),
                        factura());
        assertTrue(alegra.exitoso());
        HttpRequest solicitudAlegra = ultimaSolicitud(httpAlegra, 1);
        assertTrue(solicitudAlegra.uri().toString().endsWith("/api/v1/invoices"));
        assertTrue(solicitudAlegra.headers().firstValue("Authorization").orElseThrow().startsWith("Basic "));
        String cuerpoAlegra = cuerpo(solicitudAlegra);
        assertTrue(cuerpoAlegra.contains("\"items\""));
        assertTrue(cuerpoAlegra.contains("\"totals\""));
        assertTrue(cuerpoAlegra.contains("119000.00"));

        HttpClient httpWorld = mock(HttpClient.class);
        doReturn(respuesta(201, "{\"id\":\"W-100\"}")).when(httpWorld).send(any(), any());
        ResultadoIntegracion world = new AdaptadorWorldOffice(duracion -> httpWorld, MAPPER)
                .enviarFactura(datos(ProveedorIntegracion.WORLD_OFFICE,
                        credenciales(ProveedorIntegracion.WORLD_OFFICE), Map.of()), factura());
        assertTrue(world.exitoso());
        HttpRequest solicitudWorld = ultimaSolicitud(httpWorld, 1);
        assertTrue(solicitudWorld.uri().toString().endsWith("/api/v1/facturas"));
        assertEquals("Bearer jwt-mundo", solicitudWorld.headers().firstValue("Authorization").orElseThrow());
        String cuerpoWorld = cuerpo(solicitudWorld);
        assertTrue(cuerpoWorld.contains("\"items\""));
        assertTrue(cuerpoWorld.contains("\"totals\""));
        assertTrue(cuerpoWorld.contains("119000.00"));

        HttpClient httpHelisa = mock(HttpClient.class);
        doReturn(respuesta(201, "{\"id\":\"H-100\"}")).when(httpHelisa).send(any(), any());
        ResultadoIntegracion helisa = new AdaptadorHelisa(duracion -> httpHelisa, MAPPER)
                .enviarFactura(datos(ProveedorIntegracion.HELISA, credenciales(ProveedorIntegracion.HELISA), Map.of()),
                        factura());
        assertTrue(helisa.exitoso());
        HttpRequest solicitudHelisa = ultimaSolicitud(httpHelisa, 1);
        assertTrue(solicitudHelisa.uri().toString().startsWith("https://helisa.example/api/v1/facturas?sign="));
        assertEquals("firma-conekta", solicitudHelisa.headers().firstValue("X-Helisa-Sign").orElseThrow());
        String cuerpoHelisa = cuerpo(solicitudHelisa);
        assertTrue(cuerpoHelisa.contains("\"items\""));
        assertTrue(cuerpoHelisa.contains("\"totals\""));
        assertTrue(cuerpoHelisa.contains("119000.00"));
    }

    @Test
    void firmasInvalidasSeRechazanYValidasSeAceptan() {
        String payload = "{\"id\":\"evt-1\",\"items\":[]}";
        String marca = String.valueOf(Instant.now().getEpochSecond());
        EventoIntegracion generico = new EventoIntegracion("e1", "WEBHOOK", payload, Map.of(
                "X-Integracion-Timestamp", marca,
                "X-Integracion-Firma", firmaHex(SECRETO, marca + "." + payload)));
        EventoIntegracion mundo = new EventoIntegracion("e2", "WEBHOOK", payload, Map.of(
                "X-WorldOffice-Timestamp", marca,
                "X-WorldOffice-Signature", firmaHex(SECRETO, marca + "." + payload)));
        EventoIntegracion helisa = new EventoIntegracion("e3", "WEBHOOK", payload, Map.of(
                "X-Helisa-Timestamp", marca,
                "X-Helisa-Sign", firmaHex(SECRETO, marca + "." + payload)));

        List<AdaptadorIntegracion> adaptadores = adaptadores(mock(HttpClient.class));
        assertTrue(adaptadores.get(0).verificarFirma(datos(ProveedorIntegracion.SIIGO,
                credenciales(ProveedorIntegracion.SIIGO), Map.of()), generico));
        assertTrue(adaptadores.get(1).verificarFirma(datos(ProveedorIntegracion.ALEGRA,
                credenciales(ProveedorIntegracion.ALEGRA), Map.of()), generico));
        assertTrue(adaptadores.get(2).verificarFirma(datos(ProveedorIntegracion.WORLD_OFFICE,
                credenciales(ProveedorIntegracion.WORLD_OFFICE), Map.of()), mundo));
        assertTrue(adaptadores.get(3).verificarFirma(datos(ProveedorIntegracion.HELISA,
                credenciales(ProveedorIntegracion.HELISA), Map.of()), helisa));

        EventoIntegracion invalido = new EventoIntegracion("e4", "WEBHOOK", payload, Map.of(
                "X-Integracion-Timestamp", marca,
                "X-Integracion-Firma", "firma-invalida",
                "X-WorldOffice-Timestamp", marca,
                "X-WorldOffice-Signature", "firma-invalida",
                "X-Helisa-Timestamp", marca,
                "X-Helisa-Sign", "firma-invalida"));
        for (AdaptadorIntegracion adaptador : adaptadores) {
            assertFalse(adaptador.verificarFirma(datos(adaptador.proveedor(),
                    credenciales(adaptador.proveedor()), Map.of()), invalido));
            ResultadoIntegracion resultado = adaptador.procesarWebhook(datos(adaptador.proveedor(),
                    credenciales(adaptador.proveedor()), Map.of()), invalido);
            assertFalse(resultado.exitoso());
            assertEquals("Firma de webhook inválida.", resultado.mensaje());
        }
    }

    @Test
    void erroresHttpNoExponenCredencialesYEstanAcotados() throws Exception {
        String cuerpoError = "secreto-apiToken accessKey=secreto-siigo " + "x".repeat(400);
        for (AdaptadorIntegracion adaptador : adaptadores(mock(HttpClient.class))) {
            HttpClient http = mock(HttpClient.class);
            doReturn(respuesta(500, cuerpoError)).when(http).send(any(), any());
            AdaptadorIntegracion conHttp = conHttp(adaptador.proveedor(), http);
            ResultadoIntegracion resultado = conHttp.probarConexion(datos(adaptador.proveedor(),
                    credenciales(adaptador.proveedor()), Map.of()));
            assertFalse(resultado.exitoso());
            assertTrue(resultado.mensaje().length() <= 120, resultado.mensaje());
            assertFalse(resultado.mensaje().contains("secreto"));
            assertFalse(resultado.mensaje().contains("xxxx"));
        }
    }

    @Test
    void timeoutsSeTomanDeLaConfiguracion() throws Exception {
        HttpClient http = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"id\":1}")).when(http).send(any(), any());
        List<Duration> conexion = new ArrayList<>();
        AdaptadorAlegra alegra = new AdaptadorAlegra(duracion -> {
            conexion.add(duracion);
            return http;
        }, MAPPER);
        ResultadoIntegracion resultado = alegra.probarConexion(datos(ProveedorIntegracion.ALEGRA,
                credenciales(ProveedorIntegracion.ALEGRA),
                Map.of("connectTimeoutMs", "750", "readTimeoutMs", "1250")));
        assertTrue(resultado.exitoso());
        assertEquals(List.of(Duration.ofMillis(750)), conexion);
        assertEquals(Duration.ofMillis(1250), ultimaSolicitud(http, 1).timeout().orElseThrow());
    }

    private static List<AdaptadorIntegracion> adaptadores(HttpClient http) {
        return List.of(
                new AdaptadorSiigo(duracion -> http, MAPPER),
                new AdaptadorAlegra(duracion -> http, MAPPER),
                new AdaptadorWorldOffice(duracion -> http, MAPPER),
                new AdaptadorHelisa(duracion -> http, MAPPER));
    }

    private static AdaptadorIntegracion conHttp(ProveedorIntegracion proveedor, HttpClient http) {
        return switch (proveedor) {
            case SIIGO -> new AdaptadorSiigo(duracion -> http, MAPPER);
            case ALEGRA -> new AdaptadorAlegra(duracion -> http, MAPPER);
            case WORLD_OFFICE -> new AdaptadorWorldOffice(duracion -> http, MAPPER);
            case HELISA -> new AdaptadorHelisa(duracion -> http, MAPPER);
            default -> throw new IllegalArgumentException(String.valueOf(proveedor));
        };
    }

    private static Map<String, String> credenciales(ProveedorIntegracion proveedor) {
        Map<String, String> base = new HashMap<>();
        base.put("webhookSecret", SECRETO);
        switch (proveedor) {
            case SIIGO -> {
                base.put("username", "demo");
                base.put("accessKey", "secreto-siigo");
                base.put("url", "https://siigo.example");
            }
            case ALEGRA -> {
                base.put("email", "demo@example.com");
                base.put("apiToken", "secreto-alegra");
            }
            case WORLD_OFFICE -> {
                base.put("apiToken", "jwt-mundo");
                base.put("url", "https://world.example");
            }
            case HELISA -> {
                base.put("usuario", "demo");
                base.put("clave", "secreto-helisa");
                base.put("url", "https://helisa.example");
                base.put("sign", "firma-conekta");
                base.put("empresa", "1");
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
        return new FacturaIntegracion(
                "fac-1", "FE-001", LocalDate.of(2026, 11, 20), "cli-1", "900123456",
                new BigDecimal("100000.00"), new BigDecimal("19000.00"), new BigDecimal("119000.00"), "COP",
                List.of(new LineaFacturaIntegracion("prod-1", "Servicio DIAN", new BigDecimal("2"),
                        new BigDecimal("50000.00"), new BigDecimal("19000.00"))),
                Map.of("origen", "TEST"));
    }

    private static HttpResponse<String> respuesta(int estado, String cuerpo) {
        HttpResponse<String> respuesta = mock(HttpResponse.class);
        when(respuesta.statusCode()).thenReturn(estado);
        when(respuesta.body()).thenReturn(cuerpo);
        return respuesta;
    }

    private static HttpRequest ultimaSolicitud(HttpClient http, int cantidad) throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(cantidad)).send(captor.capture(), any());
        return captor.getAllValues().get(cantidad - 1);
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
}
