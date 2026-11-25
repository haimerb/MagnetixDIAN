package com.magnetixdian.config;

import com.magnetixdian.security.ApiKeyAuthFilter;
import com.magnetixdian.security.JwtAuthFilter;
import com.magnetixdian.security.TenantAccessFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static final List<String> ORIGENES_PREDETERMINADOS = List.of(
            "http://localhost:5173",
            "http://localhost:4173",
            "https://magnetixdian.hbarbetti-ing.workers.dev",
            "https://*.workers.dev",
            "https://magnetixdian.pages.dev",
            "https://*.pages.dev");

    private static final Map<String, String> SECRETOS_REQUERIDOS = Map.of(
            "magnetixdian.jwt.secret", "JWT_SECRET",
            "magnetixdian.crypto.key", "CRYPTO_KEY",
            "magnetixdian.crypto.key-id", "CRYPTO_KEY_ID",
            "magnetixdian.app.frontend-url", "FRONTEND_URL",
            "magnetixdian.pagos.wompi.events-secret", "WOMPI_EVENTS_SECRET",
            "magnetixdian.pagos.mercadopago.webhook-secret", "MERCADOPAGO_WEBHOOK_SECRET");

    private static final Set<String> VALORES_DE_EJEMPLO = Set.of(
            "cambiar",
            "change-me",
            "changeme",
            "replace-me",
            "example",
            "secret",
            "base64-de-32-bytes",
            "base64-de-64-bytes",
            "magnetixdian-dev-crypto-key-change-me",
            "bWFnbmV0aXhkaWFuLXN1cGVyLXNlY3JldC1rZXktZm9yLWp3dC1zaWduaW5nLTIwMjYtcGt6ZDEyM2ZiYQ==");

    private final JwtAuthFilter jwtAuthFilter;
    private final ApiKeyAuthFilter apiKeyAuthFilter;

    @Value("${magnetixdian.cors.allowed-origins:http://localhost:5173,http://localhost:4173}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ApiKeyAuthFilter apiKeyAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.apiKeyAuthFilter = apiKeyAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public ValidadorArranqueSeguro validadorArranqueSeguro(Environment environment) {
        return new ValidadorArranqueSeguro(environment);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectProvider<TenantAccessFilter> tenantAccessFilterProvider) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/webhook/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/integraciones/webhook/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/planes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/branding/{publicSlug}").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/registro", "/api/registro/", "/api/registro/registro",
                                "/api/registro/verificar", "/api/registro/reenviar",
                                "/api/auth/registro", "/api/auth/registro/verificar",
                                "/api/auth/registro/reenviar").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/registro/verificar", "/api/auth/registro/verificar").permitAll()
                        .requestMatchers("/api/registro/invitaciones/**",
                                "/api/auth/registro/invitaciones/**").authenticated()
                        .requestMatchers("/api/auth/me", "/api/auth/change-password").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        TenantAccessFilter tenantAccessFilter = tenantAccessFilterProvider.getIfAvailable();
        if (tenantAccessFilter != null) {
            http.addFilterAfter(tenantAccessFilter, JwtAuthFilter.class);
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        Set<String> origenes = new LinkedHashSet<>(ORIGENES_PREDETERMINADOS);
        if (allowedOrigins != null) {
            for (String origen : allowedOrigins.split(",")) {
                if (!origen.isBlank()) {
                    origenes.add(origen.trim());
                }
            }
        }
        config.setAllowedOriginPatterns(new ArrayList<>(origenes));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Valida la configuración sensible durante la creación del contexto: si fail-fast está
     * activo, la aplicación no arranca con secretos ausentes o de ejemplo. Los mensajes
     * nunca incluyen valores de secretos.
     */
    public static void validarConfiguracionSegura(Environment environment) {
        boolean failFast = Boolean.parseBoolean(
                environment.getProperty("magnetixdian.seguridad.fail-fast", "false"));
        if (!failFast) {
            log.warn("SEGURIDAD_FAIL_FAST=false: entorno de desarrollo; se permiten secretos de ejemplo y pasarela SIMULADO. No usar en producción.");
            return;
        }
        List<String> faltantes = new ArrayList<>();
        SECRETOS_REQUERIDOS.forEach((propiedad, variable) -> {
            String valor = environment.getProperty(propiedad);
            if (valor == null || valor.isBlank() || esValorDeEjemplo(valor)) {
                faltantes.add(variable);
            }
        });
        exigirVerdadero(environment, "magnetixdian.pagos.exigir-firma-webhook",
                "PAGOS_EXIGIR_FIRMA_WEBHOOK=true", faltantes);
        exigirVerdadero(environment, "integraciones.exigir-firma-webhook",
                "INTEGRACIONES_EXIGIR_FIRMA_WEBHOOK=true", faltantes);
        exigirVerdadero(environment, "magnetixdian.seguridad.tenant-filter-enabled",
                "TENANT_FILTER_ENABLED=true", faltantes);
        String exponerToken = environment.getProperty("magnetixdian.auth.expose-token", "true");
        if (Boolean.parseBoolean(exponerToken)) {
            faltantes.add("AUTH_EXPOSE_TOKEN=false");
        }
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "Arranque abortado: faltan variables de seguridad obligatorias: " + String.join(", ", faltantes));
        }
    }

    private static void exigirVerdadero(Environment environment, String propiedad, String variable,
                                        List<String> faltantes) {
        if (!Boolean.parseBoolean(environment.getProperty(propiedad, "false"))) {
            faltantes.add(variable);
        }
    }

    private static boolean esValorDeEjemplo(String valor) {
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        return VALORES_DE_EJEMPLO.stream().anyMatch(ejemplo -> normalizado.equals(ejemplo.toLowerCase(Locale.ROOT)))
                || normalizado.startsWith("magnetixdian-dev-")
                || normalizado.startsWith("base64-de-");
    }

    public static class ValidadorArranqueSeguro {

        public ValidadorArranqueSeguro(Environment environment) {
            validarConfiguracionSegura(environment);
        }
    }
}
