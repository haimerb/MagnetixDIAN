package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.AuthService;
import com.magnetixdian.application.AuthService.TokenRespuesta;
import com.magnetixdian.interfaces.dto.LoginRequest;
import com.magnetixdian.interfaces.dto.PerfilUsuarioDto;
import com.magnetixdian.interfaces.dto.RefreshRequest;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenRespuesta> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRespuesta> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilUsuarioDto> perfil(Authentication authentication) {
        return ResponseEntity.ok(authService.perfil(authentication.getName()));
    }
}