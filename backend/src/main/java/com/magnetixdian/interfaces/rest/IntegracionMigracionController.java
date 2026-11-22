package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioMigracionContable;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import com.magnetixdian.domain.integracion.SolicitudMigracionContable;
import com.magnetixdian.interfaces.dto.MigracionContableDto;
import com.magnetixdian.interfaces.dto.ResultadoMigracionDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/empresas/{empresaId}/integraciones/{proveedor}")
public class IntegracionMigracionController {

    private final ServicioMigracionContable servicio;
    private final AutorizacionEmpresa autorizacion;

    public IntegracionMigracionController(ServicioMigracionContable servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @PostMapping("/migrar-contabilidad")
    public MigracionContableDto migrar(@PathVariable Long empresaId,
                                       @PathVariable String proveedor,
                                       @RequestBody(required = false) SolicitudMigracionContable solicitud,
                                       Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.migrar(empresaId, parsear(proveedor), solicitud);
    }

    @GetMapping("/migraciones/{migracionId}/reporte")
    public ResultadoMigracionDto reporte(@PathVariable Long empresaId,
                                         @PathVariable String proveedor,
                                         @PathVariable Long migracionId,
                                         Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.reporte(empresaId, parsear(proveedor), migracionId);
    }

    @PostMapping("/migraciones/{migracionId}/reintentar")
    public MigracionContableDto reintentar(@PathVariable Long empresaId,
                                           @PathVariable String proveedor,
                                           @PathVariable Long migracionId,
                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.reintentar(empresaId, parsear(proveedor), migracionId);
    }

    private static ProveedorIntegracion parsear(String proveedor) {
        return ProveedorIntegracion.desde(proveedor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proveedor desconocido"));
    }
}
