package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioConfigFe;
import com.magnetixdian.interfaces.dto.ConfigFeDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de configuración DIAN por empresa.
 */
@RestController
@RequestMapping("/api/facturacion/config")
public class ConfiguracionFeController {

    private final ServicioConfigFe servicio;
    private final AutorizacionEmpresa autorizacion;

    public ConfiguracionFeController(ServicioConfigFe servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/{empresaId}")
    public ResponseEntity<ConfigFeDto> obtener(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.obtener(empresaId));
    }

    @PutMapping("/{empresaId}")
    public ResponseEntity<ConfigFeDto> guardar(@PathVariable Long empresaId, @RequestBody ConfigFeDto dto,
                                               Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.guardar(empresaId, dto));
    }
}
