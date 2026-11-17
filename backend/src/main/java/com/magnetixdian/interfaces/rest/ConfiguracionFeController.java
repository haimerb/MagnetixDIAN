package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioConfigFe;
import com.magnetixdian.interfaces.dto.ConfigFeDto;
import org.springframework.http.ResponseEntity;
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

    public ConfiguracionFeController(ServicioConfigFe servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{empresaId}")
    public ResponseEntity<ConfigFeDto> obtener(@PathVariable Long empresaId) {
        return ResponseEntity.ok(servicio.obtener(empresaId));
    }

    @PutMapping("/{empresaId}")
    public ResponseEntity<ConfigFeDto> guardar(@PathVariable Long empresaId, @RequestBody ConfigFeDto dto) {
        return ResponseEntity.ok(servicio.guardar(empresaId, dto));
    }
}
