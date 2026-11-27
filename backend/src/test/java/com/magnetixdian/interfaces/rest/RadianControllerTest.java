package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioCausacionRadian;
import com.magnetixdian.application.ServicioContabilizacion;
import com.magnetixdian.application.ServicioDocumentosRadian;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.DocumentoRadianDto;
import com.magnetixdian.interfaces.dto.ResultadoCausacionDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RadianControllerTest {

    @Mock
    private ServicioDocumentosRadian servicioDocumentos;
    @Mock
    private ServicioCausacionRadian servicioCausacion;
    @Mock
    private ServicioContabilizacion servicioContabilizacion;
    @Mock
    private AutorizacionEmpresa autorizacion;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RadianController controller = new RadianController(
                servicioDocumentos, servicioCausacion, servicioContabilizacion, autorizacion);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void importarDocumentos() throws Exception {
        String json = "[{\"direccion\":\"RECIBIDO\",\"tipoDocumento\":\"01\","
                + "\"cufeCude\":\"CUFE-1\",\"consecutivo\":\"F-001\","
                + "\"fecha\":\"2026-01-15\",\"total\":1190.00}]";
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "documentos.json", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
        when(servicioDocumentos.importar(eq(1L), eq("RECIBIDO"), any(byte[].class), anyString()))
                .thenReturn(List.of(documento()));

        mockMvc.perform(multipart("/api/empresas/1/radian/documentos/importar")
                        .file(archivo).param("direccion", "RECIBIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cufeCude").value("CUFE-1"))
                 .andExpect(jsonPath("$[0].estadoCausacion").value("PENDIENTE"))
                 .andExpect(jsonPath("$[0].estadoContabilizacion").value("CONTABILIZADO"))
                 .andExpect(jsonPath("$[0].asientoContableId").value(20));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void listarDocumentosConFiltros() throws Exception {
        when(servicioDocumentos.listar(1L, "EMITIDO", "01", "CAUSADA",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(documento()));

        mockMvc.perform(get("/api/empresas/1/radian/documentos")
                        .param("direccion", "EMITIDO")
                        .param("tipoDocumento", "01")
                        .param("estadoCausacion", "CAUSADA")
                        .param("fechaDesde", "2026-01-01")
                        .param("fechaHasta", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].consecutivo").value("F-001"));
    }

    @Test
    void causarDocumento() throws Exception {
        when(servicioCausacion.causar(eq(1L), eq(5L), nullable(SolicitudCausacionDto.class)))
                .thenReturn(new ResultadoCausacionDto(5L, 9L, "CAUSADA",
                        LocalDate.of(2026, 3, 1), new BigDecimal("1190.00"), "Causación registrada."));

        mockMvc.perform(post("/api/empresas/1/radian/documentos/5/causar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaCausacion\":\"2026-03-01\",\"observaciones\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.causacionId").value(9))
                .andExpect(jsonPath("$.estado").value("CAUSADA"));
    }

    @Test
    void causarDocumentoSinCuerpo() throws Exception {
        when(servicioCausacion.causar(eq(1L), eq(5L), nullable(SolicitudCausacionDto.class)))
                .thenReturn(new ResultadoCausacionDto(5L, 9L, "CAUSADA",
                        LocalDate.now(), new BigDecimal("1190.00"), "Causación registrada."));

        mockMvc.perform(post("/api/empresas/1/radian/documentos/5/causar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.causacionId").value(9));

        verify(servicioCausacion).causar(eq(1L), eq(5L), isNull());
    }

    @Test
    void contabilizarDocumento() throws Exception {
        when(servicioContabilizacion.contabilizar(1L, 5L))
                .thenReturn(new AsientoContableDto(20L, 1L, "AS-1-5-CAUSACION",
                        LocalDate.of(2026, 2, 20), "Causación RADIAN F-001", "RADIAN", 5L,
                        "CAUSACION", "RADIAN", 5L, "CONTABILIZADO", new BigDecimal("1190.00"),
                        new BigDecimal("1190.00"), new BigDecimal("1190.00"),
                        new BigDecimal("190.00"), new BigDecimal("50.00"),
                        OffsetDateTime.now(), List.of()));

        mockMvc.perform(post("/api/empresas/1/radian/documentos/5/contabilizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.documentoId").value(5));
    }

    private static DocumentoRadianDto documento() {
        return new DocumentoRadianDto(5L, 1L, "RECIBIDO", "01", "CUFE-1", "F-001",
                "900123456", "Empresa prueba", LocalDate.of(2026, 1, 15),
                new BigDecimal("1000.00"), new BigDecimal("190.00"), new BigDecimal("50.00"),
                new BigDecimal("1190.00"), "RECIBIDO", "PENDIENTE", "CONTABILIZADO", 20L,
                OffsetDateTime.now(), List.of(), List.of());
    }
}
