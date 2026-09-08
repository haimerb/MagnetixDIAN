package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioAdmin;
import com.magnetixdian.interfaces.dto.CalendarioDianDto;
import com.magnetixdian.interfaces.dto.CalendarioDianRequest;
import com.magnetixdian.interfaces.dto.EmpresaDto;
import com.magnetixdian.interfaces.dto.EmpresaRequest;
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

    public AdminController(ServicioAdmin servicio) {
        this.servicio = servicio;
    }

    // ---------------------------------------------------------------- Empresas

    @GetMapping("/empresas")
    public ResponseEntity<List<EmpresaDto>> listarEmpresas() {
        return ResponseEntity.ok(servicio.listarEmpresas());
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

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioDto> crearUsuario(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(servicio.crearUsuario(request));
    }

    @PutMapping("/usuarios/{id}/estado")
    public ResponseEntity<UsuarioDto> alternarEstado(@PathVariable Long id,
                                                     @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(servicio.alternarEstadoUsuario(
                id, Boolean.TRUE.equals(body.get("enabled"))));
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