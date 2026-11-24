package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioInvitaciones;
import com.magnetixdian.application.ServicioRegistro;
import com.magnetixdian.application.ServicioVerificacionEmail;
import com.magnetixdian.interfaces.dto.AceptarInvitacionRequest;
import com.magnetixdian.interfaces.dto.AceptarInvitacionResponse;
import com.magnetixdian.interfaces.dto.InvitacionTenantRequest;
import com.magnetixdian.interfaces.dto.InvitacionTenantResponse;
import com.magnetixdian.interfaces.dto.ReenviarVerificacionRequest;
import com.magnetixdian.interfaces.dto.ReenviarVerificacionResponse;
import com.magnetixdian.interfaces.dto.RegistroEmpresaRequest;
import com.magnetixdian.interfaces.dto.RegistroEmpresaResponse;
import com.magnetixdian.interfaces.dto.VerificacionEmailResponse;
import com.magnetixdian.security.AutorizacionEmpresa;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfigurationSource;

@RestController
public class RegistroController {

    private final ServicioRegistro servicio;
    private final ServicioVerificacionEmail verificacion;
    private final ServicioInvitaciones invitaciones;
    private final AutorizacionEmpresa autorizacion;

    public RegistroController(ServicioRegistro servicio,
                              ServicioVerificacionEmail verificacion,
                              ServicioInvitaciones invitaciones,
                              AutorizacionEmpresa autorizacion) {
        this.servicio = servicio;
        this.verificacion = verificacion;
        this.invitaciones = invitaciones;
        this.autorizacion = autorizacion;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain registroPublicSecurityFilterChain(
            HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .securityMatcher("/api/registro/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @PostMapping("/api/auth/registro")
    public ResponseEntity<RegistroEmpresaResponse> registrar(@Valid @RequestBody RegistroEmpresaRequest request) {
        return ResponseEntity.ok(servicio.registrar(request));
    }

    @GetMapping({"/api/registro/verificar", "/api/auth/registro/verificar"})
    public ResponseEntity<VerificacionEmailResponse> verificar(@RequestParam String token) {
        return ResponseEntity.ok(verificacion.verificar(token));
    }

    @PostMapping({"/api/registro/reenviar", "/api/auth/registro/reenviar"})
    public ResponseEntity<ReenviarVerificacionResponse> reenviar(
            @Valid @RequestBody ReenviarVerificacionRequest request) {
        return ResponseEntity.ok(verificacion.reenviar(request.email()));
    }

    @PostMapping("/api/empresas/{empresaId}/invitaciones")
    public ResponseEntity<InvitacionTenantResponse> invitar(
            @PathVariable Long empresaId,
            @Valid @RequestBody InvitacionTenantRequest request,
            Authentication authentication) {
        autorizacion.verificar(empresaId, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(invitaciones.invitar(empresaId, request));
    }

    @PostMapping({"/api/registro/invitaciones/aceptar", "/api/auth/registro/invitaciones/aceptar"})
    public ResponseEntity<AceptarInvitacionResponse> aceptar(
            @Valid @RequestBody AceptarInvitacionRequest request) {
        return ResponseEntity.ok(invitaciones.aceptar(request));
    }
}
