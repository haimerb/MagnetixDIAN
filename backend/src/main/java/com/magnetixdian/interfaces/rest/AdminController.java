package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.AuditoriaService;
import com.magnetixdian.application.ServicioAdmin;
import com.magnetixdian.application.ServicioEnvios;
import com.magnetixdian.interfaces.dto.CalendarioDianDto;
import com.magnetixdian.interfaces.dto.CalendarioDianRequest;
import com.magnetixdian.interfaces.dto.EmpresaDto;
import com.magnetixdian.interfaces.dto.EmpresaRequest;
import com.magnetixdian.interfaces.dto.EnvioFacturaDto;
import com.magnetixdian.interfaces.dto.EventoAuditoriaDto;
import com.magnetixdian.interfaces.dto.InvitacionRequest;
import com.magnetixdian.interfaces.dto.InvitacionResponse;
import com.magnetixdian.interfaces.dto.PaginaDto;
import com.magnetixdian.interfaces.dto.UsuarioDto;
import com.magnetixdian.interfaces.dto.UsuarioRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API de administración (solo rol ROLE_ADMIN): empresas clientes, usuarios
 * del sistema y calendario DIAN.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ServicioAdmin servicio;
    private final AuditoriaService auditoria;
    private final ServicioEnvios envios;

    public AdminController(ServicioAdmin servicio, AuditoriaService auditoria, ServicioEnvios envios) {
        this.servicio = servicio;
        this.auditoria = auditoria;
        this.envios = envios;
    }

    // ---------------------------------------------------------------- Empresas

    @GetMapping("/empresas")
    public ResponseEntity<List<EmpresaDto>> listarEmpresas() {
        return ResponseEntity.ok(servicio.listarEmpresas());
    }

    @GetMapping("/empresas/paginado")
    public ResponseEntity<PaginaDto<EmpresaDto>> listarEmpresasPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(servicio.listarEmpresasPaginado(page, size));
    }

    @PostMapping("/empresas")
    public ResponseEntity<EmpresaDto> crearEmpresa(@Valid @RequestBody EmpresaRequest request) {
        return ResponseEntity.ok(servicio.crearEmpresa(request));
    }

    @PutMapping("/empresas/{id}")
    public ResponseEntity<EmpresaDto> actualizarEmpresa(@PathVariable Long id,
                                                        @Valid @RequestBody EmpresaRequest request) {
        return ResponseEntity.ok(servicio.actualizarEmpresa(id, request));
    }

    @DeleteMapping("/empresas/{id}")
    public ResponseEntity<Void> eliminarEmpresa(@PathVariable Long id) {
        servicio.eliminarEmpresa(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- Usuarios

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDto>> listarUsuarios() {
        return ResponseEntity.ok(servicio.listarUsuarios());
    }

    @GetMapping("/usuarios/paginado")
    public ResponseEntity<PaginaDto<UsuarioDto>> listarUsuariosPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(servicio.listarUsuariosPaginado(page, size));
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioDto> crearUsuario(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(servicio.crearUsuario(request));
    }

    @PostMapping("/usuarios/invitar")
    public ResponseEntity<InvitacionResponse> invitarUsuario(@Valid @RequestBody InvitacionRequest request) {
        return ResponseEntity.ok(servicio.invitarUsuario(request));
    }

    @PutMapping("/usuarios/{id}/estado")
    public ResponseEntity<UsuarioDto> alternarEstado(@PathVariable Long id,
                                                     @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(servicio.alternarEstadoUsuario(
                id, Boolean.TRUE.equals(body.get("enabled"))));
    }

    // ---------------------------------------------------------------- Auditoría

    @GetMapping("/auditoria")
    public ResponseEntity<PaginaDto<EventoAuditoriaDto>> listarAuditoria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String usuario) {
        return ResponseEntity.ok(auditoria.listar(page, size, accion, usuario));
    }

    // ---------------------------------------------------------------- Transmisión

    @GetMapping("/transmision")
    public ResponseEntity<PaginaDto<EnvioFacturaDto>> listarEnvios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(envios.listar(estado, page, size));
    }

    @PostMapping("/transmision/{id}/reintentar")
    public ResponseEntity<EnvioFacturaDto> reintentarEnvio(@PathVariable Long id) {
        return ResponseEntity.ok(envios.reintentar(id, true));
    }

    // ---------------------------------------------------------------- Calendario

    @GetMapping("/calendario")
    public ResponseEntity<List<CalendarioDianDto>> listarCalendario(
            @RequestParam(required = false) Long anioGravable) {
        return ResponseEntity.ok(servicio.listarCalendario(anioGravable));
    }

    @PostMapping("/calendario")
    public ResponseEntity<CalendarioDianDto> crearCalendario(@Valid @RequestBody CalendarioDianRequest request) {
        return ResponseEntity.ok(servicio.crearCalendario(request));
    }

    @DeleteMapping("/calendario/{id}")
    public ResponseEntity<Void> eliminarCalendario(@PathVariable Long id) {
        servicio.eliminarCalendario(id);
        return ResponseEntity.noContent().build();
    }
}