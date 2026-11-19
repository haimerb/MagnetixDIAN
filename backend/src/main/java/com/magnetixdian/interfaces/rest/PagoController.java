package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioPagos;
import com.magnetixdian.interfaces.dto.CheckoutRequest;
import com.magnetixdian.interfaces.dto.PagoDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pagos de suscripción y notificaciones (webhooks) de las pasarelas.
 */
@RestController
@RequestMapping("/api")
public class PagoController {

    private final ServicioPagos servicio;
    private final AutorizacionEmpresa autorizacion;

    public PagoController(ServicioPagos servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/empresas/{empresaId}/pagos")
    public ResponseEntity<List<PagoDto>> listar(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.listar(empresaId));
    }

    @GetMapping("/empresas/{empresaId}/pagos/{referencia}")
    public ResponseEntity<PagoDto> obtener(@PathVariable Long empresaId,
                                           @PathVariable String referencia,
                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.obtener(empresaId, referencia));
    }

    @PostMapping("/empresas/{empresaId}/pagos/checkout")
    public ResponseEntity<PagoDto> checkout(@PathVariable Long empresaId,
                                            @RequestBody CheckoutRequest request,
                                            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.iniciar(empresaId, request.planCodigo(), request.proveedor()));
    }

    @PostMapping("/pagos/webhook/{proveedor}")
    public ResponseEntity<Void> webhook(@PathVariable String proveedor,
                                        @RequestBody(required = false) String payload,
                                        HttpServletRequest request) {
        servicio.procesarWebhook(proveedor, payload == null ? "" : payload, cabeceras(request));
        return ResponseEntity.ok().build();
    }

    private Map<String, String> cabeceras(HttpServletRequest request) {
        Map<String, String> cabeceras = new LinkedHashMap<>();
        Enumeration<String> nombres = request.getHeaderNames();
        while (nombres != null && nombres.hasMoreElements()) {
            String nombre = nombres.nextElement();
            cabeceras.put(nombre, request.getHeader(nombre));
        }
        String dataId = request.getParameter("data.id");
        if (dataId != null) {
            cabeceras.put("data.id", dataId);
        }
        return cabeceras;
    }
}
