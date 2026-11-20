package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioIntegraciones;
import com.magnetixdian.interfaces.dto.IntegracionEventoDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/integraciones/webhook")
public class IntegracionWebhookController {

    private final ServicioIntegraciones servicio;

    public IntegracionWebhookController(ServicioIntegraciones servicio) {
        this.servicio = servicio;
    }

    @PostMapping(value = "/{proveedor}/{token}", consumes = MediaType.ALL_VALUE)
    public IntegracionEventoDto recibir(@PathVariable String proveedor,
                                        @PathVariable String token,
                                        @RequestBody(required = false) String payload,
                                        @RequestHeader Map<String, String> headers) {
        Map<String, String> cabeceras = new LinkedHashMap<>();
        headers.forEach((clave, valor) -> cabeceras.put(clave, valor));
        return servicio.webhook(proveedor, token, payload, cabeceras);
    }
}
