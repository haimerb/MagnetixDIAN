package com.magnetixdian.security;

import com.magnetixdian.application.ServicioApiKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica peticiones que incluyen el encabezado {@code X-API-Key}. La clave
 * se resuelve al usuario propietario, de modo que hereda sus roles y la
 * autorización por empresa existente. No actúa si ya hay una autenticación.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private final ServicioApiKeys apiKeys;
    private final CustomUserDetailsService userDetailsService;

    public ApiKeyAuthFilter(ServicioApiKeys apiKeys, CustomUserDetailsService userDetailsService) {
        this.apiKeys = apiKeys;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clave = request.getHeader(HEADER);
        if (clave != null && !clave.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                apiKeys.autenticar(clave.trim()).ifPresent(username -> {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
            } catch (RuntimeException ignored) {
                // Clave inválida: continúa sin autenticación
            }
        }
        filterChain.doFilter(request, response);
    }
}
