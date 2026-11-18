package com.magnetixdian.security;

import com.magnetixdian.application.AuthService;
import com.magnetixdian.interfaces.dto.PerfilUsuarioDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Verifica que el usuario autenticado tenga acceso a la empresa solicitada.
 * Los usuarios con rol ADMIN acceden a cualquier empresa; el resto solo a la
 * empresa a la que están vinculados.
 */
@Component
public class AutorizacionEmpresa {

    public static final String ROL_ADMIN = "ROLE_ADMIN";

    private final AuthService authService;

    public AutorizacionEmpresa(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Lanza {@link AccessDeniedException} (HTTP 403) si el usuario no pertenece
     * a la empresa indicada.
     */
    public void verificar(Long empresaId, Authentication authentication) {
        if (empresaId == null) {
            throw new AccessDeniedException("No se especificó la empresa.");
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }
        PerfilUsuarioDto perfil = authService.perfil(authentication.getName());
        List<String> roles = perfil.roles();
        if (roles != null && roles.contains(ROL_ADMIN)) {
            return;
        }
        if (perfil.empresaId() == null || !perfil.empresaId().equals(empresaId)) {
            throw new AccessDeniedException("No tiene acceso a la empresa solicitada.");
        }
    }
}
