package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioSuscripciones;
import com.magnetixdian.interfaces.dto.CambiarPlanRequest;
import com.magnetixdian.interfaces.dto.PlanDto;
import com.magnetixdian.interfaces.dto.SuscripcionDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Planes disponibles y suscripción de cada empresa.
 */
@RestController
@RequestMapping("/api")
public class PlanController {

    private final ServicioSuscripciones servicio;
    private final AutorizacionEmpresa autorizacion;

    public PlanController(ServicioSuscripciones servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/planes")
    public ResponseEntity<List<PlanDto>> planes() {
        return ResponseEntity.ok(servicio.listarPlanes());
    }

    @GetMapping("/empresas/{empresaId}/suscripcion")
    public ResponseEntity<SuscripcionDto> actual(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.actual(empresaId));
    }

    @PostMapping("/empresas/{empresaId}/suscripcion")
    public ResponseEntity<SuscripcionDto> cambiar(@PathVariable Long empresaId,
                                                  @RequestBody CambiarPlanRequest request,
                                                  Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.cambiarPlan(empresaId, request.planCodigo(), request.proveedor()));
    }

    @DeleteMapping("/empresas/{empresaId}/suscripcion")
    public ResponseEntity<Void> cancelar(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        servicio.cancelar(empresaId);
        return ResponseEntity.noContent().build();
    }
}
