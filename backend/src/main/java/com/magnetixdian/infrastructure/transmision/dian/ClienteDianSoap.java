package com.magnetixdian.infrastructure.transmision.dian;

import com.magnetixdian.domain.transmision.CanalTransmision;
import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Canal DIAN directo (web services SOAP de habilitación).
 * Envía el documento firmado al servicio {@code SendBillSync}. La firma
 * WS-Security del mensaje SOAP se delega en el certificado configurado.
 */
@Component
public class ClienteDianSoap implements CanalTransmision {

    private static final String URL_HAB = "https://vpfe-hab.dian.gov.co/WcfDianCustomerServices.svc";
    private static final String URL_PROD = "https://vpfe.dian.gov.co/WcfDianCustomerServices.svc";

    @Override
    public String nombre() {
        return "DIAN";
    }

    @Override
    public ResultadoTransmision enviar(String xmlFirmado, String cufe, DatosEnvio datos, EmpresaFeConfigJpa config) {
        String endpoint = datos.ambiente() == 1 ? URL_PROD : URL_HAB;
        try {
            String fileName = (datos.prefijo() == null ? "" : datos.prefijo()) + datos.numero();
            String envelope = construirEnvelope(fileName, xmlFirmado);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "http://wcf.dian.colombia/IWcfDianCustomerServices/SendBillSync")
                    .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            boolean aceptado = response.statusCode() == 200
                    && body != null && !body.contains("Rechazado") && !body.contains("Rechazo");
            if (aceptado) {
                return ResultadoTransmision.ok(cufe, "Documento enviado a la DIAN", body);
            }
            return ResultadoTransmision.error("La DIAN respondió con estado " + response.statusCode(), body);
        } catch (Exception e) {
            return ResultadoTransmision.error("Error comunicándose con la DIAN: " + e.getMessage(), null);
        }
    }

    private String construirEnvelope(String fileName, String xmlFirmado) {
        String b64 = Base64.getEncoder().encodeToString(xmlFirmado.getBytes(StandardCharsets.UTF_8));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\""
                + " xmlns:wcf=\"http://wcf.dian.colombia\">"
                + "<soap:Header/>"
                + "<soap:Body>"
                + "<wcf:SendBillSync>"
                + "<wcf:fileName>" + fileName + "</wcf:fileName>"
                + "<wcf:contentFile>" + b64 + "</wcf:contentFile>"
                + "</wcf:SendBillSync>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }
}
