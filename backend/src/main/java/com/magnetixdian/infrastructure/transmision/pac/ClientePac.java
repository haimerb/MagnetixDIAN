package com.magnetixdian.infrastructure.transmision.pac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.transmision.CanalTransmision;
import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class ClientePac implements CanalTransmision {

    private final RestClient restClient;
    private final ObjectMapper mapper;

    public ClientePac(ObjectMapper mapper,
                      @Value("${magnetixdian.pac.connect-timeout-ms:5000}") long connectTimeoutMs,
                      @Value("${magnetixdian.pac.read-timeout-ms:30000}") long readTimeoutMs) {
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String nombre() {
        return "PAC";
    }

    @Override
    public ResultadoTransmision enviar(String xmlFirmado, String cufe, DatosEnvio datos, EmpresaFeConfigJpa config) {
        String baseUrl = config.getPacBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return ResultadoTransmision.error("Canal PAC sin URL configurada", null);
        }
        try {
            URI uri = URI.create(baseUrl.replaceAll("/+$", "") + "/documents");
            if (uri.getScheme() == null || (!"https".equalsIgnoreCase(uri.getScheme())
                    && !"http".equalsIgnoreCase(uri.getScheme()))) {
                return ResultadoTransmision.error("URL del PAC inválida", null);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("number", nvl(datos.numeroCompleto()));
            payload.put("cufe", nvl(cufe));
            payload.put("testSetId", nvl(config.getTestSetId()));
            payload.put("xml", Base64.getEncoder().encodeToString(xmlFirmado.getBytes(StandardCharsets.UTF_8)));

            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", cufe != null && !cufe.isBlank() ? cufe : UUID.randomUUID().toString());
            String apiKey = config.getPacApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                spec.header("Authorization", "Bearer " + apiKey);
            }

            JsonNode respuesta = spec.body(mapper.writeValueAsString(payload))
                    .retrieve()
                    .body(JsonNode.class);
            String crudo = respuesta == null ? "" : respuesta.toString();
            if (respuesta == null || respuesta.isMissingNode()) {
                return ResultadoTransmision.error("El PAC devolvió una respuesta vacía", crudo);
            }
            boolean aceptado = estadoAceptado(respuesta);
            if (aceptado) {
                String cufeRemoto = texto(respuesta, "cufe");
                return ResultadoTransmision.ok(cufeRemoto != null ? cufeRemoto : cufe,
                        "Documento enviado al PAC", crudo);
            }
            String mensaje = texto(respuesta, "message");
            if (mensaje == null) {
                mensaje = texto(respuesta, "error");
            }
            return ResultadoTransmision.error(mensaje != null ? mensaje : "El PAC rechazó el documento", crudo);
        } catch (RestClientResponseException e) {
            return ResultadoTransmision.error("El PAC respondió con estado " + e.getStatusCode().value(),
                    e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResultadoTransmision.error("Error comunicándose con el PAC: " + e.getMessage(), null);
        }
    }

    private boolean estadoAceptado(JsonNode respuesta) {
        JsonNode nodo = respuesta;
        for (String clave : new String[]{"data", "document", "resultado"}) {
            if (respuesta.has(clave) && respuesta.get(clave).isObject()) {
                nodo = respuesta.get(clave);
                break;
            }
        }
        String estado = firstTexto(nodo, "status", "estado", "state");
        if (estado != null) {
            String normalizado = estado.toUpperCase(Locale.ROOT);
            return normalizado.contains("ACEPT") || normalizado.contains("APROB") || "OK".equals(normalizado);
        }
        Boolean aceptado = firstBooleano(nodo, "accepted", "aceptado", "aprobado");
        if (aceptado != null) {
            return aceptado;
        }
        return !nodo.toString().toLowerCase(Locale.ROOT).contains("rechaz");
    }

    private String texto(JsonNode nodo, String clave) {
        return firstTexto(nodo, clave);
    }

    private String firstTexto(JsonNode nodo, String... claves) {
        for (String clave : claves) {
            JsonNode valor = nodo.get(clave);
            if (valor != null && valor.isValueNode() && !valor.asText().isBlank()) {
                return valor.asText();
            }
        }
        return null;
    }

    private Boolean firstBooleano(JsonNode nodo, String... claves) {
        for (String clave : claves) {
            JsonNode valor = nodo.get(clave);
            if (valor != null && valor.isBoolean()) {
                return valor.asBoolean();
            }
        }
        return null;
    }

    private static String nvl(String valor) {
        return valor == null ? "" : valor;
    }
}
