package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioBranding;
import com.magnetixdian.interfaces.dto.BrandingDto;
import com.magnetixdian.interfaces.dto.BrandingRequest;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BrandingController {

    private final ServicioBranding servicio;
    private final AutorizacionEmpresa autorizacion;

    public BrandingController(ServicioBranding servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/api/empresas/{empresaId}/branding")
    public ResponseEntity<BrandingDto> obtener(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.obtener(empresaId));
    }

    @PutMapping("/api/empresas/{empresaId}/branding")
    public ResponseEntity<BrandingDto> guardar(@PathVariable Long empresaId,
                                               @RequestBody BrandingRequest request,
                                               Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.guardar(empresaId, request));
    }

    @GetMapping("/api/branding/{publicSlug}")
    public ResponseEntity<BrandingDto> publico(@PathVariable String publicSlug) {
        return servicio.porSlug(publicSlug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
