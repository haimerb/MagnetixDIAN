package com.magnetixdian.seguridad;

import com.magnetixdian.application.JwtService;
import com.magnetixdian.application.ServicioApiKeys;
import com.magnetixdian.config.SecurityConfig;
import com.magnetixdian.security.ApiKeyAuthFilter;
import com.magnetixdian.security.CustomUserDetailsService;
import com.magnetixdian.security.JwtAuthFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(classes = {SecurityConfig.class, SecurityConfigRulesTest.FiltrosConfig.class,
        SecurityConfigRulesTest.Endpoints.class})
@WebAppConfiguration
class SecurityConfigRulesTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void webhookPlanesYBrandingPublicoSonPublicos() throws Exception {
        mockMvc.perform(post("/api/pagos/webhook/wompi")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/planes")).andExpect(status().isOk());
        mockMvc.perform(get("/api/branding/empresa-demo")).andExpect(status().isOk());
    }

    @Test
    void brandingConMasDeUnSegmentoNoEsPublico() throws Exception {
        mockMvc.perform(get("/api/branding/empresa-demo/privado")).andExpect(denegado());
    }

    @Test
    void registroVerificarYReenviarSonPublicosPeroInvitacionesNo() throws Exception {
        mockMvc.perform(post("/api/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/registro/verificar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/registro/verificar?token=abc")).andExpect(status().isOk());
        mockMvc.perform(post("/api/registro/reenviar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/registro/invitaciones")).andExpect(denegado());
    }

    @Test
    void todoLoDemasExigeAutenticacion() throws Exception {
        mockMvc.perform(get("/api/facturas")).andExpect(denegado());
        mockMvc.perform(post("/api/pagos/checkout")).andExpect(denegado());
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(denegado());
    }

    @Test
    @WithMockUser(roles = "CONTADOR")
    void usuarioAutenticadoNoEntraAAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEntraAAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isOk());
    }

    @Test
    void corsIncluyeWorkersYPagesYOpcionesEstaPermitido() throws Exception {
        mockMvc.perform(options("/api/planes")
                        .header("Origin", "https://magnetixdian.hbarbetti-ing.workers.dev")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());

        SecurityConfig config = new SecurityConfig(mock(JwtAuthFilter.class), mock(ApiKeyAuthFilter.class));
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://extra.example");
        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());
        assertNotNull(cors.checkOrigin("https://magnetixdian.hbarbetti-ing.workers.dev"));
        assertNotNull(cors.checkOrigin("https://magnetixdian.pages.dev"));
        assertNotNull(cors.checkOrigin("https://otro.workers.dev"));
        assertNotNull(cors.checkOrigin("https://extra.example"));
        assertNull(cors.checkOrigin("https://malicioso.example"));
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
        assertFalse(cors.getAllowedMethods().isEmpty());
    }

    private static ResultMatcher denegado() {
        return resultado -> {
            int estado = resultado.getResponse().getStatus();
            assertTrue(estado == 401 || estado == 403, "Se esperaba 401/403 pero fue " + estado);
        };
    }

    @Configuration
    @EnableWebMvc
    static class FiltrosConfig {

        @Bean
        JwtAuthFilter jwtAuthFilter() {
            return new JwtAuthFilter(mock(JwtService.class), mock(CustomUserDetailsService.class));
        }

        @Bean
        ApiKeyAuthFilter apiKeyAuthFilter() {
            return new ApiKeyAuthFilter(mock(ServicioApiKeys.class), mock(CustomUserDetailsService.class));
        }
    }

    @RestController
    static class Endpoints {

        @RequestMapping(value = {
                "/api/planes",
                "/api/branding/{publicSlug}",
                "/api/registro",
                "/api/registro/verificar",
                "/api/registro/reenviar",
                "/api/registro/invitaciones",
                "/api/auth/registro",
                "/api/facturas",
                "/api/admin/usuarios",
                "/actuator/health"
        }, method = {RequestMethod.GET, RequestMethod.POST})
        String respuesta() {
            return "ok";
        }

        @RequestMapping(value = "/api/pagos/webhook/{proveedor}", method = RequestMethod.POST)
        String webhook() {
            return "ok";
        }

        @RequestMapping(value = "/api/pagos/checkout", method = RequestMethod.POST)
        String checkout() {
            return "ok";
        }
    }
}
