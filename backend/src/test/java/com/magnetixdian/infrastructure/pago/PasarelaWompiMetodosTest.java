package com.magnetixdian.infrastructure.pago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.pago.CheckoutPago;
import com.magnetixdian.domain.pago.MetodoPago;
import com.magnetixdian.domain.pago.SolicitudPago;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasarelaWompiMetodosTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void brebQrUsaApiOficialYDevuelveQr() throws Exception {
        ColaRespuestas cola = new ColaRespuestas()
                .responder("""
                        {"data":{"presigned_acceptance":{"acceptance_token":"accept-1"},
                        "presigned_personal_data_auth":{"acceptance_token":"personal-1"}}}
                        """)
                .responder("""
                        {"data":{"id":"tx-1"}}
                        """)
                .responder("""
                        {"data":{"payment_method":{"extra":{}}}}
                        """)
                .responder("""
                        {"data":{"payment_method":{"extra":{"qr_image":"BASE64_QR","qr_id":"qr-1"}}}}
                        """);

        CheckoutPago checkout = pasarela(cola).crearCheckout(solicitud(MetodoPago.BREB_QR, "Plan PRO"));

        assertEquals(CheckoutPago.TIPO_QR, checkout.tipo());
        assertEquals("qr-1", checkout.qrId());
        assertEquals("BASE64_QR", checkout.qrImage());
        assertEquals("tx-1", checkout.transaccionId());
        assertEquals("wompi-qr:qr-1", checkout.url());
        assertEquals(4, cola.solicitudes().size());
        assertEquals("/merchants/info", cola.solicitudes().get(0).uri().getPath());
        assertEquals("pub_test_x", cola.solicitudes().get(0).headers().getFirst("x-merchant-public-key"));
        assertEquals("/v1/transactions", cola.solicitudes().get(1).uri().getPath());
        assertEquals("Bearer prv_test_x", cola.solicitudes().get(1).headers().getFirst("Authorization"));
        JsonNode cuerpo = mapper.readTree(cola.solicitudes().get(1).cuerpo());
        assertEquals("BANCOLOMBIA_QR", cuerpo.path("payment_method").path("type").asText());
        assertEquals("Plan PRO", cuerpo.path("payment_method").path("payment_description").asText());
        assertEquals("accept-1", cuerpo.path("acceptance_token").asText());
        assertEquals("personal-1", cuerpo.path("accept_personal_auth").asText());
        assertTrue(cuerpo.path("payment_method").path("acceptance_token").isMissingNode());
        assertEquals("APPROVED", cuerpo.path("payment_method").path("sandbox_status").asText());
        assertTrue(cuerpo.path("sandbox_status").isMissingNode());
        assertEquals(64, cuerpo.path("signature").asText().length());
        assertEquals("/v1/transactions/tx-1", cola.solicitudes().get(2).uri().getPath());
    }

    @Test
    void brebQrAceptaQrEnPaymentMethod() throws Exception {
        ColaRespuestas cola = new ColaRespuestas()
                .responder("""
                        {"data":{"presigned_acceptance":{"acceptance_token":"accept-qr"},
                        "presigned_personal_data_auth":{"acceptance_token":"personal-qr"}}}
                        """)
                .responder("""
                        {"data":{"id":"tx-qr"}}
                        """)
                .responder("""
                        {"data":{"payment_method":{"type":"BANCOLOMBIA_QR",
                        "qr_image":"BASE64_QR_DIRECTO","qr_id":"qr-directo"}}}
                        """);

        CheckoutPago checkout = pasarela(cola).crearCheckout(solicitud(MetodoPago.BREB_QR, "Plan PRO"));

        assertEquals(CheckoutPago.TIPO_QR, checkout.tipo());
        assertEquals("qr-directo", checkout.qrId());
        assertEquals("BASE64_QR_DIRECTO", checkout.qrImage());
        assertEquals("wompi-qr:qr-directo", checkout.url());
    }

    @Test
    void davipLataUsaApiOficialYDevuelveUrlOtp() throws Exception {
        ColaRespuestas cola = new ColaRespuestas()
                .responder("""
                        {"data":{"presigned_acceptance":{"acceptance_token":"accept-2"},
                        "presigned_personal_data_auth":{"acceptance_token":"personal-2"}}}
                        """)
                .responder("""
                        {"data":{"id":"tx-2"}}
                        """)
                .responder("""
                        {"data":{"payment_method":{"extra":
                        {"url":"https://wompi.test/otp/tx-2"}}}}
                        """);

        CheckoutPago checkout = pasarela(cola).crearCheckout(solicitud(MetodoPago.DAVIPLATA,
                "Plan Profesional de suscripción mensual"));

        assertEquals(CheckoutPago.TIPO_OTP, checkout.tipo());
        assertEquals("https://wompi.test/otp/tx-2", checkout.url());
        assertEquals("tx-2", checkout.transaccionId());
        JsonNode cuerpo = mapper.readTree(cola.solicitudes().get(1).cuerpo());
        assertEquals("DAVIPLATA", cuerpo.path("payment_method").path("type").asText());
        assertEquals("DAVIPLATA", cuerpo.path("payment_method_type").asText());
        assertTrue(cuerpo.path("payment_method").path("payment_method_type").isMissingNode());
        assertEquals("CC", cuerpo.path("payment_method").path("user_legal_id_type").asText());
        assertEquals("1099888777", cuerpo.path("payment_method").path("user_legal_id").asText());
        assertEquals(30, cuerpo.path("payment_method").path("payment_description").asText().length());
        assertNotNull(cuerpo.path("signature").asText());
        assertTrue(cuerpo.path("signature").asText().matches("[0-9a-f]{64}"));
    }

    private PasarelaWompi pasarela(ColaRespuestas cola) {
        RestClient http = RestClient.builder()
                .baseUrl("https://wompi.test")
                .requestFactory(cola)
                .build();
        return new PasarelaWompi("pub_test_x", "integrity-test", "events-test", "https://checkout.test/p/",
                "prv_test_x", "https://wompi.test", 300, false, 3, 0, "APPROVED", mapper, http);
    }

    private static SolicitudPago solicitud(MetodoPago metodo, String descripcion) {
        return new SolicitudPago(1L, 2L, "WOMPI-123", "PRO", new BigDecimal("99000"), "COP",
                "Plan Profesional", "http://localhost/suscripcion", metodo, "3107654321", null, null,
                "CC", "1099888777", "Ana Perez", "ana@example.com", "3107654321", descripcion);
    }

    private record SolicitudHttp(URI uri, HttpMethod method, HttpHeaders headers, String cuerpo) {
    }

    private static final class ColaRespuestas implements ClientHttpRequestFactory {

        private final Deque<String> respuestas = new ArrayDeque<>();
        private final List<SolicitudHttp> solicitudes = new ArrayList<>();

        ColaRespuestas responder(String cuerpo) {
            respuestas.add(cuerpo);
            return this;
        }

        List<SolicitudHttp> solicitudes() {
            return solicitudes;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            HttpHeaders headers = new HttpHeaders();
            ByteArrayOutputStream cuerpoEscrito = new ByteArrayOutputStream();
            return new ClientHttpRequest() {
                @Override
                public HttpMethod getMethod() {
                    return httpMethod;
                }

                @Override
                public URI getURI() {
                    return uri;
                }

                @Override
                public HttpHeaders getHeaders() {
                    return headers;
                }

                @Override
                public ByteArrayOutputStream getBody() {
                    return cuerpoEscrito;
                }

                @Override
                public ClientHttpResponse execute() {
                    String respuesta = respuestas.poll();
                    if (respuesta == null) {
                        throw new AssertionError("No hay respuesta HTTP simulada para " + uri);
                    }
                    solicitudes.add(new SolicitudHttp(uri, httpMethod, new HttpHeaders(headers),
                            cuerpoEscrito.toString(StandardCharsets.UTF_8)));
                    return respuesta(respuesta);
                }
            };
        }

        private static ClientHttpResponse respuesta(String cuerpo) {
            return new ClientHttpResponse() {
                private final HttpHeaders headers = new HttpHeaders();

                @Override
                public HttpStatusCode getStatusCode() {
                    return HttpStatus.OK;
                }

                @Override
                public String getStatusText() {
                    return HttpStatus.OK.getReasonPhrase();
                }

                @Override
                public void close() {
                }

                @Override
                public InputStream getBody() {
                    return new ByteArrayInputStream(cuerpo.getBytes(StandardCharsets.UTF_8));
                }

                @Override
                public HttpHeaders getHeaders() {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    return headers;
                }
            };
        }
    }
}
