package com.magnetixdian.onboarding;

import com.magnetixdian.application.ServicioInvitaciones;
import com.magnetixdian.application.ServicioRegistro;
import com.magnetixdian.application.ServicioVerificacionEmail;
import com.magnetixdian.domain.dian.ModoOperacionDian;
import com.magnetixdian.interfaces.dto.AceptarInvitacionResponse;
import com.magnetixdian.interfaces.dto.InvitacionTenantResponse;
import com.magnetixdian.interfaces.dto.ReenviarVerificacionResponse;
import com.magnetixdian.interfaces.dto.RegistroEmpresaResponse;
import com.magnetixdian.interfaces.dto.VerificacionEmailResponse;
import com.magnetixdian.interfaces.rest.RegistroController;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistroControllerTest {

    private ServicioRegistro registro;
    private ServicioVerificacionEmail verificacion;
    private ServicioInvitaciones invitaciones;
    private AutorizacionEmpresa autorizacion;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registro = mock(ServicioRegistro.class);
        verificacion = mock(ServicioVerificacionEmail.class);
        invitaciones = mock(ServicioInvitaciones.class);
        autorizacion = mock(AutorizacionEmpresa.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new RegistroController(registro, verificacion, invitaciones, autorizacion))
                .build();
    }

    @Test
    void registrarDevuelveDatosDelNuevoTenant() throws Exception {
        when(registro.registrar(any())).thenReturn(new RegistroEmpresaResponse(1L, 2L, "cliente1", "PRO",
                "PENDIENTE_VERIFICACION", ModoOperacionDian.ADQUIRIDO_CLIENTE, "Cuenta creada."));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nit":"90012345678","razonSocial":"Empresa Prueba SAS","nombre":"Cliente Uno",
                                 "email":"cliente1@ejemplo.com","username":"cliente1","password":"clave1234",
                                 "aceptaTerminos":true,"website":"","planCode":"PRO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(1))
                .andExpect(jsonPath("$.planCode").value("PRO"))
                .andExpect(jsonPath("$.estadoTenant").value("PENDIENTE_VERIFICACION"))
                .andExpect(jsonPath("$.modoOperacionDian").value("ADQUIRIDO_CLIENTE"));
        verify(registro).registrar(any());
    }

    @Test
    void registrarRechazaPlanVacio() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nit":"90012345678","razonSocial":"Empresa Prueba SAS","nombre":"Cliente Uno",
                                 "email":"cliente1@ejemplo.com","username":"cliente1","password":"clave1234",
                                 "aceptaTerminos":true,"website":"","planCode":""}
                                """))
                .andExpect(status().isBadRequest());
        verify(registro, never()).registrar(any());
    }

    @Test
    void verificarExpondeTambienBajoAuth() throws Exception {
        when(verificacion.verificar("abc")).thenReturn(new VerificacionEmailResponse(1L, "ACTIVO", true,
                "Correo verificado."));

        mockMvc.perform(get("/api/registro/verificar").param("token", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(1))
                .andExpect(jsonPath("$.emailVerificado").value(true));
        mockMvc.perform(get("/api/auth/registro/verificar").param("token", "abc"))
                .andExpect(status().isOk());
        verify(verificacion, org.mockito.Mockito.times(2)).verificar("abc");
    }

    @Test
    void reenviarDevuelveResultado() throws Exception {
        when(verificacion.reenviar("cliente1@ejemplo.com"))
                .thenReturn(new ReenviarVerificacionResponse(true, "Si la cuenta existe...", null));

        mockMvc.perform(post("/api/registro/reenviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente1@ejemplo.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enviado").value(true));
        verify(verificacion).reenviar("cliente1@ejemplo.com");
    }

    @Test
    void invitarValidaAccesoALaEmpresa() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(invitaciones.invitar(eq(42L), any())).thenReturn(new InvitacionTenantResponse(42L, 5L,
                "invitado", "invitado@ejemplo.com", "CONTADOR", OffsetDateTime.now().plusDays(7),
                "Invitación enviada."));

        mockMvc.perform(post("/api/empresas/42/invitaciones")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invitado@ejemplo.com","nombre":"Invitado","rol":"CONTADOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empresaId").value(42))
                .andExpect(jsonPath("$.rol").value("CONTADOR"));
        verify(autorizacion).verificar(42L, authentication);
    }

    @Test
    void aceptarInvitacionExpondeTambienBajoAuth() throws Exception {
        when(invitaciones.aceptar(any())).thenReturn(new AceptarInvitacionResponse(42L, 5L, "invitado",
                "LECTOR", "Invitación aceptada."));
        String body = "{\"token\":\"abc\",\"password\":\"clave1234\"}";

        mockMvc.perform(post("/api/registro/invitaciones/aceptar")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("LECTOR"));
        mockMvc.perform(post("/api/auth/registro/invitaciones/aceptar")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(invitaciones, org.mockito.Mockito.times(2)).aceptar(any());
    }
}
