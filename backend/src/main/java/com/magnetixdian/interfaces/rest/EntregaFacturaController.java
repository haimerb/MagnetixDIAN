package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioEntregaFactura;
import com.magnetixdian.interfaces.dto.EntregaWhatsAppRequest;
import com.magnetixdian.interfaces.dto.EntregaWhatsAppResponse;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntregaFacturaController {

    private final ServicioEntregaFactura servicio;
    private final AutorizacionEmpresa autorizacion;

    public EntregaFacturaController(ServicioEntregaFactura servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @PostMapping("/api/empresas/{empresaId}/facturacion/{facturaId}/entregar-whatsapp")
    public ResponseEntity<EntregaWhatsAppResponse> entregar(@PathVariable Long empresaId,
                                                            @PathVariable Long facturaId,
                                                            @RequestBody EntregaWhatsAppRequest request,
                                                            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        String usuario = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(servicio.entregar(empresaId, facturaId, request, usuario));
    }
}
