package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioHabilitacionDianWizard;
import com.magnetixdian.interfaces.dto.AuditoriaDianDto;
import com.magnetixdian.interfaces.dto.DocumentoTestSetDianDto;
import com.magnetixdian.interfaces.dto.HabilitacionDianEmpresaDto;
import com.magnetixdian.interfaces.dto.ResultadoTestSetDianDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HabilitacionDianControllerTest {

    @Mock
    private ServicioHabilitacionDianWizard servicio;
    @Mock
    private AutorizacionEmpresa autorizacion;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HabilitacionDianController(servicio, autorizacion)).build();
    }

    @Test
    void exponeHabilitacionGet() throws Exception {
        when(servicio.obtener(1L)).thenReturn(habilitacion());

        mockMvc.perform(get("/api/empresas/1/dian/habilitacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(1))
                .andExpect(jsonPath("$.modo").value("ADQUIRIDO_CLIENTE"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void exponeHabilitacionPut() throws Exception {
        when(servicio.guardar(eq(1L), any())).thenReturn(habilitacion());

        mockMvc.perform(put("/api/empresas/1/dian/habilitacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "modo": "ADQUIRIDO_CLIENTE",
                                  "resolucionNumero": "RES-1",
                                  "resolucionFecha": "2026-01-01",
                                  "prefijo": "FE",
                                  "rangoDesde": 1,
                                  "rangoHasta": 1000,
                                  "testSetId": "TS-1",
                                  "softwareId": "SW-1",
                                  "notificarAdquirente": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolucionNumero").value("RES-1"));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void exponeDocumentosDelTestSet() throws Exception {
        when(servicio.documentos(1L)).thenReturn(List.of(
                new DocumentoTestSetDianDto("TEST-01", "01", "Factura 1", "PENDIENTE", null)));

        mockMvc.perform(get("/api/empresas/1/dian/test-set/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackId").value("TEST-01"))
                .andExpect(jsonPath("$[0].tipoDocumento").value("01"));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void exponeEnvioDelTestSet() throws Exception {
        when(servicio.enviar(eq(1L), any())).thenReturn(resultado());

        mockMvc.perform(post("/api/empresas/1/dian/test-set/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentos\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testSetId").value("TS-1"))
                .andExpect(jsonPath("$.estado").value("TEST_SET_APROBADO"));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void exponeConsultaDelTestSet() throws Exception {
        when(servicio.consultar(1L, "TS-1")).thenReturn(resultado());

        mockMvc.perform(get("/api/empresas/1/dian/test-set/TS-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.aprobados").value(10));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void exponeSwitchover() throws Exception {
        when(servicio.switchover(eq(1L), any())).thenReturn(produccion());

        mockMvc.perform(post("/api/empresas/1/dian/switchover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmacion": "APRUEBO_SWITCHOVER",
                                  "testSetId": "TS-1",
                                  "resolucionNumero": "RES-1",
                                  "observaciones": "Pruebas aprobadas"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PRODUCCION"));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void exponeAuditoria() throws Exception {
        when(servicio.obtener(1L)).thenReturn(habilitacion());

        mockMvc.perform(get("/api/empresas/1/dian/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("TEST_SET_ENVIADO"));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    private HabilitacionDianEmpresaDto habilitacion() {
        OffsetDateTime fecha = OffsetDateTime.parse("2026-11-25T10:00:00Z");
        return new HabilitacionDianEmpresaDto(1L, "ADQUIRIDO_CLIENTE", "BORRADOR", "RES-1",
                LocalDate.of(2026, 1, 1), "FE", 1, 1000, "TS-1", "SW-1", false,
                "certificado.p12", null, null,
                List.of(new AuditoriaDianDto(5L, "TEST_SET_ENVIADO", "trackId=TRACK-1", "admin", fecha)));
    }

    private HabilitacionDianEmpresaDto produccion() {
        OffsetDateTime fecha = OffsetDateTime.parse("2026-11-25T10:00:00Z");
        return new HabilitacionDianEmpresaDto(1L, "ADQUIRIDO_CLIENTE", "PRODUCCION", "RES-1",
                LocalDate.of(2026, 1, 1), "FE", 1, 1000, "TS-1", "SW-1", false,
                "certificado.p12", fecha, fecha,
                List.of(new AuditoriaDianDto(5L, "SWITCHOVER_PRODUCCION", "trackId=TRACK-1", "admin", fecha)));
    }

    private ResultadoTestSetDianDto resultado() {
        return new ResultadoTestSetDianDto("TS-1", "TEST_SET_APROBADO", 10, 10, 0, List.of(
                new DocumentoTestSetDianDto("TEST-01", "01", "Factura 1", "APROBADO", "Aceptado")));
    }
}
