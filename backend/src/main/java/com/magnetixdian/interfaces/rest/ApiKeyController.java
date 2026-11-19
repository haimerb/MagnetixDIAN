package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioApiKeys;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.ApiKeyCreadaDto;
import com.magnetixdian.interfaces.dto.ApiKeyDto;
import com.magnetixdian.interfaces.dto.CrearApiKeyRequest;
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
 * Claves de API por empresa. La clave en claro solo se devuelve al crearla.
 */
@RestController
@RequestMapping("/api/empresas/{empresaId}/api-keys")
public class ApiKeyController {

    private final ServicioApiKeys servicio;
    private final AutorizacionEmpresa autorizacion;
    private final UsuarioRepository usuarioRepo;

    public ApiKeyController(ServicioApiKeys servicio, AutorizacionEmpresa autorizacion,
                            UsuarioRepository usuarioRepo) {
        this.servicio = servicio;
        this.autorizacion = autorizacion;
        this.usuarioRepo = usuarioRepo;
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyDto>> listar(@PathVariable Long empresaId, Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.ok(servicio.listar(empresaId));
    }

    @PostMapping
    public ResponseEntity<ApiKeyCreadaDto> crear(@PathVariable Long empresaId,
                                                 @RequestBody CrearApiKeyRequest request,
                                                 Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        Long usuarioId = usuarioRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."))
                .getId();
        return ResponseEntity.ok(servicio.crear(empresaId, usuarioId, request.nombre()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revocar(@PathVariable Long empresaId, @PathVariable Long id,
                                        Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        servicio.revocar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
