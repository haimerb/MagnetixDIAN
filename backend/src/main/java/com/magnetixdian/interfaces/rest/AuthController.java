package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.AuthService;
import com.magnetixdian.application.AuthService.TokenRespuesta;
import com.magnetixdian.application.ServicioRecuperacion;
import com.magnetixdian.interfaces.dto.CambioPasswordRequest;
import com.magnetixdian.interfaces.dto.LoginRequest;
import com.magnetixdian.interfaces.dto.PerfilUsuarioDto;
import com.magnetixdian.interfaces.dto.RefreshRequest;
import com.magnetixdian.interfaces.dto.SolicitudResetRequest;
import com.magnetixdian.interfaces.dto.SolicitudResetResponse;
import com.magnetixdian.interfaces.dto.TokenPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ServicioRecuperacion recuperacion;

    public AuthController(AuthService authService, ServicioRecuperacion recuperacion) {
        this.authService = authService;
        this.recuperacion = recuperacion;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenRespuesta> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRespuesta> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request,
                                       Authentication authentication) {
        String username = authentication == null ? null : authentication.getName();
        authService.logout(request == null ? null : request.refreshToken(), username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<SolicitudResetResponse> forgotPassword(
            @Valid @RequestBody SolicitudResetRequest request) {
        return ResponseEntity.ok(recuperacion.solicitarReset(request.identificador()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody TokenPasswordRequest request) {
        recuperacion.restablecer(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<Void> acceptInvite(@Valid @RequestBody TokenPasswordRequest request) {
        recuperacion.aceptarInvitacion(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody CambioPasswordRequest request,
                                               Authentication authentication) {
        authService.cambiarPassword(authentication.getName(), request.passwordActual(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilUsuarioDto> perfil(Authentication authentication) {
        return ResponseEntity.ok(authService.perfil(authentication.getName()));
    }
}
