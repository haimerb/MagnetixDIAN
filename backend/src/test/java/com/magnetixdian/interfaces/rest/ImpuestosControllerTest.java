package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioCausacionImpuestos;
import com.magnetixdian.application.ServicioCertificadosImpuesto;
import com.magnetixdian.application.ServicioDeclaracionesImpuesto;
import com.magnetixdian.application.ServicioResponsabilidadesFiscales;
import com.magnetixdian.application.ServicioTarifasImpuesto;
import com.magnetixdian.interfaces.dto.CalendarioTributarioDto;
import com.magnetixdian.interfaces.dto.CausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.DeclaracionImpuestoDto;
import com.magnetixdian.interfaces.dto.ResultadoCalculoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudDeclaracionImpuestoDto;
import com.magnetixdian.interfaces.dto.TarifaImpuestoDto;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
class ImpuestosControllerTest {

    @Mock
    private ServicioTarifasImpuesto servicioTarifas;
    @Mock
    private ServicioResponsabilidadesFiscales servicioResponsabilidades;
    @Mock
    private ServicioCausacionImpuestos servicioCausacion;
    @Mock
    private ServicioCertificadosImpuesto servicioCertificados;
    @Mock
    private ServicioDeclaracionesImpuesto servicioDeclaraciones;
    @Mock
    private AutorizacionEmpresa autorizacion;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ImpuestosController controller = new ImpuestosController(
                servicioTarifas, servicioResponsabilidades, servicioCausacion,
                servicioCertificados, servicioDeclaraciones, autorizacion);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listarTarifas() throws Exception {
        when(servicioTarifas.listar(null, null)).thenReturn(List.of(new TarifaImpuestoDto(
                1L, "01", "IVA", 2026, null, new BigDecimal("0.1900"),
                BigDecimal.ZERO, null, true)));

        mockMvc.perform(get("/api/empresas/1/impuestos/tarifas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].impuestoCodigo").value("01"));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void calcularImpuesto() throws Exception {
        when(servicioTarifas.calcular(
                "05", 2026, "2004", new BigDecimal("1000000.00")))
                .thenReturn(new ResultadoCalculoImpuestoDto(
                        "05", "Retención en la fuente", 2026, "2004",
                        new BigDecimal("1000000.00"), new BigDecimal("52374.00"),
                        new BigDecimal("157122.00"), null, new BigDecimal("1000000.00"),
                        new BigDecimal("0.0040"), true, new BigDecimal("4000.00")));

        mockMvc.perform(post("/api/empresas/1/impuestos/calcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "impuestoCodigo": "05",
                                  "anio": 2026,
                                  "conceptoCodigo": "2004",
                                  "base": 1000000.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplica").value(true))
                .andExpect(jsonPath("$.valor").value(4000.00));

        verify(servicioTarifas).calcular(eq("05"), eq(2026), eq("2004"),
                eq(new BigDecimal("1000000.00")));
    }

    @Test
    void causarImpuesto() throws Exception {
        when(servicioCausacion.causar(eq(1L), any(SolicitudCausacionImpuestoDto.class)))
                .thenReturn(new CausacionImpuestoDto(
                        10L, 1L, "01", "FACTURA", 5L, null, "2026-03",
                        LocalDate.of(2026, 3, 15), null, new BigDecimal("1000000.00"),
                        new BigDecimal("0.1900"), new BigDecimal("190000.00"),
                        "CAUSADO", BigDecimal.ZERO, null));

        mockMvc.perform(post("/api/empresas/1/impuestos/causaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "impuestoCodigo": "01",
                                  "origen": "FACTURA",
                                  "origenId": 5,
                                  "fecha": "2026-03-15",
                                  "baseImponible": 1000000.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("2026-03"))
                .andExpect(jsonPath("$.estado").value("CAUSADO"));
    }

    @Test
    void generarDeclaracionEnBorrador() throws Exception {
        when(servicioDeclaraciones.generarBorrador(eq(1L), any(SolicitudDeclaracionImpuestoDto.class)))
                .thenReturn(new DeclaracionImpuestoDto(
                        7L, 1L, "01", 2026, "2026-03",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                        LocalDate.of(2026, 4, 20), "BORRADOR",
                        new BigDecimal("190000.00"), BigDecimal.ZERO, new BigDecimal("190000.00")));

        mockMvc.perform(post("/api/empresas/1/impuestos/declaraciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "impuestoCodigo": "01",
                                  "anioGravable": 2026,
                                  "periodo": "2026-03",
                                  "fechaDesde": "2026-03-01",
                                  "fechaHasta": "2026-03-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.fechaLimite").isNotEmpty());
    }

    @Test
    void listarCalendarioTributario() throws Exception {
        when(servicioDeclaraciones.listarCalendario(2026)).thenReturn(List.of(
                new CalendarioTributarioDto(
                        1L, "01", 2026, "MENSUAL", "2026-03", LocalDate.of(2026, 4, 20))));

        mockMvc.perform(get("/api/empresas/1/impuestos/calendario").param("anio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].periodo").value("2026-03"));
    }
}
