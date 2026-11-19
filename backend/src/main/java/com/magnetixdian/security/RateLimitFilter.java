package com.magnetixdian.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Límite simple de peticiones por IP para los endpoints de autenticación:
 * 20 peticiones por minuto (ventana deslizante en memoria).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE = 20;
    private static final Duration VENTANA = Duration.ofMinutes(1);
    private static final int MAX_IPS = 10_000;

    private final Map<String, Deque<Instant>> peticiones = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/auth") || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = ip(request);
        Instant limite = Instant.now().minus(VENTANA);
        Deque<Instant> marcas = peticiones.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        while (marcas.peekFirst() != null && marcas.peekFirst().isBefore(limite)) {
            marcas.pollFirst();
        }
        if (marcas.size() >= LIMITE) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Demasiadas solicitudes. Intente de nuevo en un minuto.\"}");
            return;
        }
        marcas.addLast(Instant.now());
        if (peticiones.size() > MAX_IPS) {
            peticiones.clear();
        }
        filterChain.doFilter(request, response);
    }

    private static String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
