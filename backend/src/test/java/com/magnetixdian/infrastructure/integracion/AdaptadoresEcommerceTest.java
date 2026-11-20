package com.magnetixdian.infrastructure.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.integracion.ClienteIntegracion;
import com.magnetixdian.domain.integracion.DatosConexionIntegracion;
import com.magnetixdian.domain.integracion.EventoIntegracion;
import com.magnetixdian.domain.integracion.ProductoIntegracion;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import com.magnetixdian.domain.integracion.ResultadoIntegracion;
import com.magnetixdian.infrastructure.integracion.shopify.AdaptadorShopify;
import com.magnetixdian.infrastructure.integracion.woocommerce.AdaptadorWooCommerce;
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
import java.util.ArrayList;
import java.util.Base64;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdaptadoresEcommerceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void sinCredencialesNoEstaConfiguradoYNoLlamaHttp() {
        HttpClient http = mock(HttpClient.class);
        AdaptadorShopify shopify = shopify(http);
        AdaptadorWooCommerce woo = woo(http);
        DatosConexionIntegracion datosShopify = datos(ProveedorIntegracion.SHOPIFY,
                Map.of("shop", "tienda.myshopify.com"), Map.of());
        DatosConexionIntegracion datosWoo = datos(ProveedorIntegracion.WOOCOMMERCE,
                Map.of("url", "https://tienda.example", "consumerKey", "ck_secreto"), Map.of());

        assertFalse(shopify.configurada(datosShopify));
        assertFalse(woo.configurada(datosWoo));
        assertFalse(shopify.probarConexion(datosShopify).exitoso());
        assertFalse(woo.probarConexion(datosWoo).exitoso());
        verifyNoInteractions(http);
    }

    @Test
    void firmaInvalidaOSecretoAusenteSeRechaza() {
        String payload = "{\"id\":101,\"first_name\":\"Ana\"}";
        AdaptadorShopify shopify = shopify(mock(HttpClient.class));
        AdaptadorWooCommerce woo = woo(mock(HttpClient.class));
        EventoIntegracion eventoShopify = new EventoIntegracion("e1", "WEBHOOK", payload, Map.of(
                "X-Shopify-Topic", "customers/create",
                "X-Shopify-Hmac-Sha256", Base64.getEncoder().encodeToString("invalida".getBytes(StandardCharsets.UTF_8))));
        EventoIntegracion eventoWoo = new EventoIntegracion("e2", "WEBHOOK", payload, Map.of(
                "X-WC-Webhook-Topic", "customer.created",
                "X-WC-Webhook-Signature", Base64.getEncoder().encodeToString("invalida".getBytes(StandardCharsets.UTF_8))));

        assertFalse(shopify.verificarFirma(datosShopify(), eventoShopify));
        assertFalse(woo.verificarFirma(datosWoo(), eventoWoo));
        assertFalse(shopify.procesarWebhook(datosShopify(), eventoShopify).exitoso());
        assertFalse(woo.procesarWebhook(datosWoo(), eventoWoo).exitoso());

        DatosConexionIntegracion sinSecretoShopify = datos(ProveedorIntegracion.SHOPIFY,
                Map.of("shop", "tienda.myshopify.com", "accessToken", "token-secreto"), Map.of());
        DatosConexionIntegracion sinSecretoWoo = datos(ProveedorIntegracion.WOOCOMMERCE,
                Map.of("url", "https://tienda.example", "consumerKey", "ck_secreto",
                        "consumerSecret", "cs_secreto"), Map.of());
        EventoIntegracion conFirma = new EventoIntegracion("e3", "WEBHOOK", payload,
                Map.of("X-Shopify-Hmac-Sha256", firma("secreto-shopify", payload),
                        "X-WC-Webhook-Signature", firma("secreto-woo", payload)));
        assertFalse(shopify.verificarFirma(sinSecretoShopify, conFirma));
        assertFalse(woo.verificarFirma(sinSecretoWoo, conFirma));
    }

    @Test
    void webhooksConFirmaValidaSeMapeanADominio() throws Exception {
        AdaptadorShopify shopify = shopify(mock(HttpClient.class));
        AdaptadorWooCommerce woo = woo(mock(HttpClient.class));
        String clienteShopify = """
                {"id":101,"first_name":"Ana","last_name":"Perez","email":"ana@example.com",
                 "tags":"900123456","default_address":{"address1":"Calle 1","city":"Bogota"}}
                """;
        EventoIntegracion eventoClienteShopify = new EventoIntegracion("e1", "WEBHOOK", clienteShopify,
                Map.of("X-Shopify-Topic", "customers/create",
                        "X-Shopify-Hmac-Sha256", firma("secreto-shopify", clienteShopify)));
        assertTrue(shopify.verificarFirma(datosShopify(), eventoClienteShopify));
        ResultadoIntegracion cliente = shopify.procesarWebhook(datosShopify(), eventoClienteShopify);
        assertTrue(cliente.exitoso());
        assertEquals("101", cliente.idExterno());
        assertEquals("CLIENTE", cliente.detalles().get("entidad"));

        String productoShopify = """
                {"id":202,"title":"Producto Demo","variants":[{"sku":"SKU-1","price":"19900.00","taxable":true}]}
                """;
        EventoIntegracion eventoProductoShopify = new EventoIntegracion("e2", "WEBHOOK", productoShopify,
                Map.of("X-Shopify-Topic", "products/update",
                        "X-Shopify-Hmac-Sha256", firma("secreto-shopify", productoShopify)));
        ResultadoIntegracion producto = shopify.procesarWebhook(datosShopify(), eventoProductoShopify);
        assertTrue(producto.exitoso());
        assertEquals("202", producto.idExterno());
        assertEquals("PRODUCTO", producto.detalles().get("entidad"));

        String clienteWoo = """
                {"id":303,"first_name":"Luis","last_name":"Gomez","email":"luis@example.com",
                 "username":"900111","billing":{"company":"Demo SAS","phone":"3001",
                 "address_1":"Calle 2","city":"Medellin"}}
                """;
        EventoIntegracion eventoClienteWoo = new EventoIntegracion("e3", "WEBHOOK", clienteWoo,
                Map.of("X-WC-Webhook-Topic", "customer.created",
                        "X-WC-Webhook-Signature", firma("secreto-woo", clienteWoo)));
        assertTrue(woo.verificarFirma(datosWoo(), eventoClienteWoo));
        ResultadoIntegracion clienteW = woo.procesarWebhook(datosWoo(), eventoClienteWoo);
        assertTrue(clienteW.exitoso());
        assertEquals("303", clienteW.idExterno());

        String productoWoo = """
                {"id":404,"sku":"SKU-W","name":"Servicio","price":"25000","tax_status":"taxable"}
                """;
        EventoIntegracion eventoProductoWoo = new EventoIntegracion("e4", "WEBHOOK", productoWoo,
                Map.of("X-WC-Webhook-Topic", "product.updated",
                        "X-WC-Webhook-Signature", firma("secreto-woo", productoWoo)));
        ResultadoIntegracion productoW = woo.procesarWebhook(datosWoo(), eventoProductoWoo);
        assertTrue(productoW.exitoso());
        assertEquals("404", productoW.idExterno());
    }

    @Test
    void sincronizarClienteYProductoGeneranSolicitudesMapeadas() throws Exception {
        HttpClient httpCliente = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"customer\":{\"id\":\"900\"}}")).when(httpCliente).send(any(), any());
        ResultadoIntegracion clienteShopify = shopify(httpCliente).sincronizarCliente(datosShopify(),
                new ClienteIntegracion(null, "900123456", "Ana Perez", "ana@example.com", "300123", "Calle 1"));
        assertTrue(clienteShopify.exitoso());
        assertEquals("900", clienteShopify.idExterno());
        HttpRequest solicitudCliente = capturar(httpCliente);
        assertTrue(solicitudCliente.uri().toString().endsWith("/admin/api/2024-10/customers.json"));
        assertEquals("POST", solicitudCliente.method());
        assertTrue(solicitudCliente.headers().firstValue("X-Shopify-Access-Token").isPresent());
        String cuerpoCliente = cuerpo(solicitudCliente);
        assertTrue(cuerpoCliente.contains("\"first_name\":\"Ana\""));
        assertTrue(cuerpoCliente.contains("\"tags\":\"900123456\""));

        HttpClient httpProducto = mock(HttpClient.class);
        doReturn(respuesta(201, "{\"id\":\"555\"}")).when(httpProducto).send(any(), any());
        ResultadoIntegracion productoWoo = woo(httpProducto).sincronizarProducto(datosWoo(),
                new ProductoIntegracion(null, "SKU-9", "Servicio DIAN", new BigDecimal("45000"), "IVA_19"));
        assertTrue(productoWoo.exitoso());
        assertEquals("555", productoWoo.idExterno());
        HttpRequest solicitudProducto = capturar(httpProducto);
        assertTrue(solicitudProducto.uri().toString().endsWith("/wp-json/wc/v3/products"));
        assertTrue(solicitudProducto.headers().firstValue("Authorization").orElseThrow().startsWith("Basic "));
        String cuerpoProducto = cuerpo(solicitudProducto);
        assertTrue(cuerpoProducto.contains("\"sku\":\"SKU-9\""));
        assertTrue(cuerpoProducto.contains("\"regular_price\":\"45000\""));
    }

    @Test
    void probarConexionUsaTimeoutsYCabecerasDelProveedor() throws Exception {
        HttpClient httpShopify = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"shop\":{\"id\":55,\"name\":\"Demo\"}}"))
                .when(httpShopify).send(any(), any());
        List<Duration> tiemposConexion = new ArrayList<>();
        AdaptadorShopify shopify = new AdaptadorShopify(duracion -> {
            tiemposConexion.add(duracion);
            return httpShopify;
        }, MAPPER);
        ResultadoIntegracion conexionShopify = shopify.probarConexion(datos(ProveedorIntegracion.SHOPIFY,
                shopifyCredenciales(), Map.of("connectTimeoutMs", "750", "readTimeoutMs", "1250")));
        assertTrue(conexionShopify.exitoso());
        assertEquals("55", conexionShopify.idExterno());
        assertEquals(List.of(Duration.ofMillis(750)), tiemposConexion);
        HttpRequest solicitudShopify = capturar(httpShopify);
        assertTrue(solicitudShopify.uri().toString().endsWith("/admin/api/2024-10/shop.json"));
        assertEquals(Duration.ofMillis(1250), solicitudShopify.timeout().orElseThrow());
        assertTrue(solicitudShopify.headers().firstValue("X-Shopify-Access-Token").isPresent());

        HttpClient httpWoo = mock(HttpClient.class);
        doReturn(respuesta(200, "{\"environment\":{\"home_url\":\"https://tienda.example\"}}"))
                .when(httpWoo).send(any(), any());
        AdaptadorWooCommerce woo = new AdaptadorWooCommerce(duracion -> httpWoo, MAPPER);
        ResultadoIntegracion conexionWoo = woo.probarConexion(datos(ProveedorIntegracion.WOOCOMMERCE,
                wooCredenciales(), Map.of("readTimeoutMs", "900")));
        assertTrue(conexionWoo.exitoso());
        assertEquals("https://tienda.example", conexionWoo.idExterno());
        HttpRequest solicitudWoo = capturar(httpWoo);
        assertTrue(solicitudWoo.uri().toString().endsWith("/wp-json/wc/v3/system_status"));
        assertEquals(Duration.ofMillis(900), solicitudWoo.timeout().orElseThrow());
        assertTrue(solicitudWoo.headers().firstValue("Authorization").orElseThrow().startsWith("Basic "));
    }

    @Test
    void erroresHttpNoExponenCredenciales() throws Exception {
        HttpClient http = mock(HttpClient.class);
        doReturn(respuesta(401, "token-secreto rechazado")).when(http).send(any(), any());
        ResultadoIntegracion resultado = shopify(http).probarConexion(datosShopify());
        assertFalse(resultado.exitoso());
        assertFalse(resultado.mensaje().contains("token-secreto"));
    }

    private static AdaptadorShopify shopify(HttpClient http) {
        return new AdaptadorShopify(duracion -> http, MAPPER);
    }

    private static AdaptadorWooCommerce woo(HttpClient http) {
        return new AdaptadorWooCommerce(duracion -> http, MAPPER);
    }

    private static DatosConexionIntegracion datosShopify() {
        return datos(ProveedorIntegracion.SHOPIFY, shopifyCredenciales(), Map.of());
    }

    private static DatosConexionIntegracion datosWoo() {
        return datos(ProveedorIntegracion.WOOCOMMERCE, wooCredenciales(), Map.of());
    }

    private static Map<String, String> shopifyCredenciales() {
        return Map.of("shop", "tienda.myshopify.com", "accessToken", "token-secreto",
                "webhookSecret", "secreto-shopify");
    }

    private static Map<String, String> wooCredenciales() {
        return Map.of("url", "https://tienda.example", "consumerKey", "ck_secreto",
                "consumerSecret", "cs_secreto", "webhookSecret", "secreto-woo");
    }

    private static DatosConexionIntegracion datos(ProveedorIntegracion proveedor,
                                                  Map<String, String> credenciales,
                                                  Map<String, String> opciones) {
        return new DatosConexionIntegracion(1L, proveedor, credenciales, opciones, true);
    }

    private static HttpResponse<String> respuesta(int estado, String cuerpo) {
        HttpResponse<String> respuesta = mock(HttpResponse.class);
        when(respuesta.statusCode()).thenReturn(estado);
        when(respuesta.body()).thenReturn(cuerpo);
        return respuesta;
    }

    private static HttpRequest capturar(HttpClient http) throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(captor.capture(), any());
        return captor.getValue();
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

    private static String firma(String secreto, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
