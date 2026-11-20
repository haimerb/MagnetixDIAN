package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioIntegraciones;
import com.magnetixdian.domain.integracion.ProveedorIntegracion;
import com.magnetixdian.domain.integracion.ResultadoIntegracion;
import com.magnetixdian.interfaces.dto.IntegracionConfigDto;
import com.magnetixdian.interfaces.dto.IntegracionConfigRequest;
import com.magnetixdian.interfaces.dto.IntegracionEventoDto;
import com.magnetixdian.interfaces.dto.IntegracionSincronizacionRequest;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/empresas/{empresaId}/integraciones")
public class IntegracionController {

    private final ServicioIntegraciones servicio;
    private final AutorizacionEmpresa autorizacion;

    public IntegracionController(ServicioIntegraciones servicio, AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
    }

    @GetMapping
    public List<IntegracionConfigDto> listar(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.listar(empresaId);
    }

    @PutMapping("/{proveedor}")
    public IntegracionConfigDto guardar(@PathVariable Long empresaId,
                                        @PathVariable String proveedor,
                                        @RequestBody IntegracionConfigRequest solicitud,
                                        Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.guardar(empresaId, parsear(proveedor), solicitud);
    }

    @PostMapping("/{proveedor}/probar")
    public ResultadoIntegracion probar(@PathVariable Long empresaId,
                                       @PathVariable String proveedor,
                                       Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.probar(empresaId, parsear(proveedor));
    }

    @PostMapping("/{proveedor}/sincronizar")
    public ResultadoIntegracion sincronizar(@PathVariable Long empresaId,
                                            @PathVariable String proveedor,
                                            @RequestBody IntegracionSincronizacionRequest solicitud,
                                            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.sincronizar(empresaId, parsear(proveedor), solicitud);
    }

    @GetMapping("/eventos")
    public List<IntegracionEventoDto> eventos(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.eventos(empresaId);
    }

    @PostMapping("/eventos/{eventoId}/reintentar")
    public IntegracionEventoDto reintentar(@PathVariable Long empresaId,
                                           @PathVariable Long eventoId,
                                           Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return servicio.reintentar(empresaId, eventoId);
    }

    private static ProveedorIntegracion parsear(String proveedor) {
        return ProveedorIntegracion.desde(proveedor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proveedor desconocido"));
    }
}
