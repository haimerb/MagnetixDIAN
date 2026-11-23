package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioGeneracionExogena;
import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
import com.magnetixdian.interfaces.dto.ResultadoGeneracionExogenaDto;
import com.magnetixdian.interfaces.dto.ResultadoValidacionExogenaDto;
import com.magnetixdian.interfaces.dto.SolicitudGeneracionExogenaDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExogenaControllerTest {

    @Mock
    private ServicioGeneracionExogena servicio;
    @Mock
    private AutorizacionEmpresa autorizacion;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExogenaController(servicio, autorizacion)).build();
    }

    @Test
    void listarFormatosOficiales() throws Exception {
        when(servicio.formatosOficiales()).thenReturn(List.of(new FormatoExogenaRegistry().obtener("1125")));

        mockMvc.perform(get("/api/empresas/1/exogena/formatos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("1125"))
                .andExpect(jsonPath("$[0].activo").value(true));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void generarXmlUsaElFormatoDeLaRuta() throws Exception {
        when(servicio.generar(eq(1L), any())).thenReturn(new ResultadoGeneracionExogenaDto(
                "1125", "13", "Formato 1125", 1, "<xml/>", "XSD local", false));

        mockMvc.perform(post("/api/empresas/1/exogena/1125/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "formato": "1001",
                                  "anioGravable": 2025,
                                  "filas": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formato").value("1125"))
                .andExpect(jsonPath("$.prevalidacionOficial").value(false));

        ArgumentCaptor<SolicitudGeneracionExogenaDto> captor =
                ArgumentCaptor.forClass(SolicitudGeneracionExogenaDto.class);
        verify(servicio).generar(eq(1L), captor.capture());
        assertEquals("1125", captor.getValue().formato());
    }

    @Test
    void validarXml() throws Exception {
        when(servicio.validarXml("5253", "<xml/>")).thenReturn(new ResultadoValidacionExogenaDto(
                "5253", false, "XML inválido", false));

        mockMvc.perform(post("/api/empresas/1/exogena/5253/validar-xml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"xml\":\"<xml/>\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formato").value("5253"))
                .andExpect(jsonPath("$.valido").value(false));
    }
}
