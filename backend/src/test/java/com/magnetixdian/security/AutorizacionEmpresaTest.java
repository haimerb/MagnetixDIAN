package com.magnetixdian.security;

import com.magnetixdian.application.AuthService;
import com.magnetixdian.interfaces.dto.PerfilUsuarioDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutorizacionEmpresaTest {

    private AuthService authService;
    private AutorizacionEmpresa autorizacion;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        autorizacion = new AutorizacionEmpresa(authService);
        authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("usuario");
    }

    private void perfil(Long empresaId, List<String> roles) {
        when(authService.perfil("usuario"))
                .thenReturn(new PerfilUsuarioDto(1L, "usuario", "Usuario", "u@x.co", roles, empresaId, "900", "Empresa"));
    }

    @Test
    void adminAccedeACualquierEmpresa() {
        perfil(null, List.of("ROLE_ADMIN"));
        assertDoesNotThrow(() -> autorizacion.verificar(99L, authentication));
    }

    @Test
    void usuarioAccedeASuPropiaEmpresa() {
        perfil(5L, List.of("ROLE_CLIENTE"));
        assertDoesNotThrow(() -> autorizacion.verificar(5L, authentication));
    }

    @Test
    void usuarioNoAccedeAOtraEmpresa() {
        perfil(5L, List.of("ROLE_CLIENTE"));
        assertThrows(AccessDeniedException.class, () -> autorizacion.verificar(6L, authentication));
    }

    @Test
    void usuarioSinEmpresaNoAccede() {
        perfil(null, List.of("ROLE_CONTADOR"));
        assertThrows(AccessDeniedException.class, () -> autorizacion.verificar(1L, authentication));
    }

    @Test
    void empresaNulaEsRechazada() {
        assertThrows(AccessDeniedException.class, () -> autorizacion.verificar(null, authentication));
    }

    @Test
    void usuarioNoAutenticadoEsRechazado() {
        when(authentication.isAuthenticated()).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> autorizacion.verificar(1L, authentication));
    }
}
