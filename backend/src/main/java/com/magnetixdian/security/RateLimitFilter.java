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
    private static final int LIMITE_REGISTRO = 5;
    private static final Duration VENTANA_REGISTRO = Duration.ofMinutes(10);
    private static final int MAX_IPS = 10_000;

    private final Map<String, Deque<Instant>> peticiones = new ConcurrentHashMap<>();
    private final Map<String, Deque<Instant>> registros = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/auth") || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean registro = uri.startsWith("/api/auth/registro");
        int limite = registro ? LIMITE_REGISTRO : LIMITE;
        Duration ventana = registro ? VENTANA_REGISTRO : VENTANA;
        Map<String, Deque<Instant>> contadores = registro ? registros : peticiones;

        String ip = ip(request);
        Instant limiteTiempo = Instant.now().minus(ventana);
        Deque<Instant> marcas = contadores.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        while (marcas.peekFirst() != null && marcas.peekFirst().isBefore(limiteTiempo)) {
            marcas.pollFirst();
        }
        if (marcas.size() >= limite) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Demasiadas solicitudes. Intente de nuevo más tarde.\"}");
            return;
        }
        marcas.addLast(Instant.now());
        if (contadores.size() > MAX_IPS) {
            contadores.clear();
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
