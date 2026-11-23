package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioCausacionImpuestos;
import com.magnetixdian.application.ServicioCertificadosImpuesto;
import com.magnetixdian.application.ServicioDeclaracionesImpuesto;
import com.magnetixdian.application.ServicioResponsabilidadesFiscales;
import com.magnetixdian.application.ServicioTarifasImpuesto;
import com.magnetixdian.interfaces.dto.CalendarioTributarioDto;
import com.magnetixdian.interfaces.dto.CausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.CertificadoImpuestoDto;
import com.magnetixdian.interfaces.dto.DeclaracionImpuestoDto;
import com.magnetixdian.interfaces.dto.PagoImpuestoDto;
import com.magnetixdian.interfaces.dto.ResponsabilidadFiscalDto;
import com.magnetixdian.interfaces.dto.ResultadoCalculoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudCertificadoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudDeclaracionImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudResponsabilidadFiscalDto;
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
    void registrarResponsabilidadFiscal() throws Exception {
        when(servicioResponsabilidades.registrar(eq(1L), any(SolicitudResponsabilidadFiscalDto.class)))
                .thenReturn(new ResponsabilidadFiscalDto(
                        2L, 1L, 9L, "IVA", "RESPONSABLE IVA",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));

        mockMvc.perform(post("/api/empresas/1/impuestos/responsabilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "terceroId": 9,
                                  "impuestoCodigo": "IVA",
                                  "codigo": "RESPONSABLE IVA",
                                  "desde": "2026-01-01",
                                  "hasta": "2026-12-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("RESPONSABLE IVA"));
    }

    @Test
    void listarResponsabilidadesFiscales() throws Exception {
        when(servicioResponsabilidades.listar(1L)).thenReturn(List.of(
                new ResponsabilidadFiscalDto(2L, 1L, 9L, "IVA", "RESPONSABLE IVA",
                        LocalDate.of(2026, 1, 1), null)));

        mockMvc.perform(get("/api/empresas/1/impuestos/responsabilidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].impuestoCodigo").value("IVA"));
    }

    @Test
    void listarCausacionesPorTercero() throws Exception {
        when(servicioCausacion.listarPorTercero(1L, 9L)).thenReturn(List.of(
                new CausacionImpuestoDto(10L, 1L, "01", "FACTURA", 5L, 9L, "2026-03",
                        LocalDate.of(2026, 3, 15), null, new BigDecimal("1000000.00"),
                        new BigDecimal("0.1900"), new BigDecimal("190000.00"),
                        "CAUSADO", BigDecimal.ZERO, null)));

        mockMvc.perform(get("/api/empresas/1/impuestos/causaciones").param("terceroId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].terceroId").value(9))
                .andExpect(jsonPath("$[0].estado").value("CAUSADO"));
    }

    @Test
    void registrarPagoDeCausacion() throws Exception {
        when(servicioCausacion.registrarPago(eq(1L), eq(10L), any()))
                .thenReturn(new PagoImpuestoDto(3L, 10L, LocalDate.of(2026, 3, 20),
                        new BigDecimal("190000.00"), "COMP-1"));

        mockMvc.perform(post("/api/empresas/1/impuestos/causaciones/10/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fecha": "2026-03-20",
                                  "valor": 190000.00,
                                  "comprobante": "COMP-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprobante").value("COMP-1"));
    }

    @Test
    void anularCausacion() throws Exception {
        when(servicioCausacion.anular(1L, 10L)).thenReturn(new CausacionImpuestoDto(
                10L, 1L, "01", "FACTURA", 5L, null, "2026-03",
                LocalDate.of(2026, 3, 15), null, new BigDecimal("1000000.00"),
                new BigDecimal("0.1900"), BigDecimal.ZERO.setScale(2),
                "ANULADO", BigDecimal.ZERO, "Anulada por prueba"));

        mockMvc.perform(post("/api/empresas/1/impuestos/causaciones/10/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));
    }

    @Test
    void emitirCertificadoDeImpuesto() throws Exception {
        when(servicioCertificados.emitir(eq(1L), any(SolicitudCertificadoImpuestoDto.class)))
                .thenReturn(new CertificadoImpuestoDto(
                        4L, 1L, 9L, "IVA", 2026, "CERT-1",
                        new BigDecimal("100.00"), LocalDate.of(2026, 3, 31)));

        mockMvc.perform(post("/api/empresas/1/impuestos/certificados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "terceroId": 9,
                                  "impuestoCodigo": "IVA",
                                  "anio": 2026,
                                  "numero": "CERT-1",
                                  "valor": 100.00,
                                  "fechaEmision": "2026-03-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("CERT-1"));
    }

    @Test
    void listarCertificadosDeImpuesto() throws Exception {
        when(servicioCertificados.listar(1L, 2026)).thenReturn(List.of(
                new CertificadoImpuestoDto(4L, 1L, 9L, "IVA", 2026, "CERT-1",
                        new BigDecimal("100.00"), LocalDate.of(2026, 3, 31))));

        mockMvc.perform(get("/api/empresas/1/impuestos/certificados").param("anio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("CERT-1"));
    }

    @Test
    void listarDeclaraciones() throws Exception {
        when(servicioDeclaraciones.listar(1L, 2026)).thenReturn(List.of(
                new DeclaracionImpuestoDto(7L, 1L, "IVA", 2026, "2026-03",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                        LocalDate.of(2026, 4, 20), "BORRADOR",
                        new BigDecimal("190000.00"), BigDecimal.ZERO, new BigDecimal("190000.00"))));

        mockMvc.perform(get("/api/empresas/1/impuestos/declaraciones").param("anio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].periodo").value("2026-03"));
    }

    @Test
    void presentarDeclaracion() throws Exception {
        when(servicioDeclaraciones.marcarPresentada(1L, 7L)).thenReturn(new DeclaracionImpuestoDto(
                7L, 1L, "IVA", 2026, "2026-03",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 20), "PRESENTADA",
                new BigDecimal("190000.00"), BigDecimal.ZERO, BigDecimal.ZERO.setScale(2)));

        mockMvc.perform(post("/api/empresas/1/impuestos/declaraciones/7/presentar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PRESENTADA"));
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
