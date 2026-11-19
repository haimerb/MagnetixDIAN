package com.magnetixdian.infrastructure.transmision.pac;

import com.magnetixdian.domain.transmision.CanalTransmision;
import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;

/**
 * Canal PAC (Proveedor Autorizado Calificado) vía API REST configurable.
 * Compatible con proveedores tipo Factus/Alanube que reciben el XML firmado y
 * devuelven el estado de validación.
 */
@Component
public class ClientePac implements CanalTransmision {

    private final RestClient restClient = RestClient.create();

    @Override
    public String nombre() {
        return "PAC";
    }

    @Override
    public ResultadoTransmision enviar(String xmlFirmado, String cufe, DatosEnvio datos, EmpresaFeConfigJpa config) {
        if (config.getPacBaseUrl() == null || config.getPacBaseUrl().isBlank()) {
            return ResultadoTransmision.error("Canal PAC sin URL configurada", null);
        }
        try {
            String url = config.getPacBaseUrl().replaceAll("/+$", "") + "/documents";
            String body = "{\"number\":\"" + datos.numeroCompleto() + "\","
                    + "\"cufe\":\"" + cufe + "\","
                    + "\"testSetId\":\"" + nvl(config.getTestSetId()) + "\","
                    + "\"xml\":\"" + Base64.getEncoder().encodeToString(xmlFirmado.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "\"}";

            String respuesta = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + nvl(config.getPacApiKey()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            boolean aceptado = respuesta != null && !respuesta.toLowerCase().contains("rechaz");
            if (aceptado) {
                return ResultadoTransmision.ok(cufe, "Documento enviado al PAC", respuesta);
            }
            return ResultadoTransmision.error("El PAC rechazó el documento", respuesta);
        } catch (Exception e) {
            return ResultadoTransmision.error("Error comunicándose con el PAC: " + e.getMessage(), null);
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
