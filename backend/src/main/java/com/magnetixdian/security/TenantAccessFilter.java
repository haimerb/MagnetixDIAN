package com.magnetixdian.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TenantAccessFilter extends OncePerRequestFilter {

    private static final Pattern RUTA_EMPRESA = Pattern.compile("^/api/empresas/(\\d+)(?:/|$)");

    private final AutorizacionEmpresa autorizacion;

    @Value("${magnetixdian.seguridad.tenant-filter-enabled:false}")
    private boolean enabled;

    public TenantAccessFilter(AutorizacionEmpresa autorizacion) {
        this.autorizacion = autorizacion;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (enabled) {
            Matcher ruta = RUTA_EMPRESA.matcher(request.getRequestURI());
            if (ruta.find()) {
                try {
                    autorizacion.verificar(Long.valueOf(ruta.group(1)),
                            SecurityContextHolder.getContext().getAuthentication());
                } catch (AccessDeniedException ex) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
