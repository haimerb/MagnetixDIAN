package com.magnetixdian.interfaces.rest;

import com.magnetixdian.application.ServicioConciliacion;
import com.magnetixdian.application.ServicioContabilizacion;
import com.magnetixdian.interfaces.dto.AsientoContableDto;
import com.magnetixdian.interfaces.dto.ConciliacionLineaDto;
import com.magnetixdian.interfaces.dto.ResultadoConciliacionDto;
import com.magnetixdian.security.AutorizacionEmpresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContabilidadControllerTest {

    @Mock
    private ServicioContabilizacion servicioContabilizacion;
    @Mock
    private ServicioConciliacion servicioConciliacion;
    @Mock
    private AutorizacionEmpresa autorizacion;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ContabilidadController controller = new ContabilidadController(
                servicioContabilizacion, servicioConciliacion, autorizacion);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listarAsientos() throws Exception {
        when(servicioContabilizacion.listar(1L)).thenReturn(List.of(new AsientoContableDto(
                20L, 1L, "AS-1-5-CAUSACION", LocalDate.of(2026, 2, 20), "Causación RADIAN",
                "RADIAN", 5L, "CAUSACION", "CONTABILIZADO", new BigDecimal("1190.00"),
                new BigDecimal("1190.00"), new BigDecimal("1190.00"), new BigDecimal("190.00"),
                new BigDecimal("50.00"), OffsetDateTime.now(), List.of())));

        mockMvc.perform(get("/api/empresas/1/contabilidad/asientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("AS-1-5-CAUSACION"));

        verify(autorizacion).verificar(eq(1L), nullable(Authentication.class));
    }

    @Test
    void conciliarDocumentos() throws Exception {
        when(servicioConciliacion.conciliar(1L, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), "RECIBIDO", "01"))
                .thenReturn(new ResultadoConciliacionDto(3L, 1L,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                        1, 1, 0, 0, BigDecimal.ZERO.setScale(2),
                        List.of(new ConciliacionLineaDto(5L, "F-001", 20L, "COINCIDE",
                                "INFO", "TOTAL", new BigDecimal("1190.00"),
                                new BigDecimal("1190.00"), BigDecimal.ZERO.setScale(2), "ok"))));

        mockMvc.perform(post("/api/empresas/1/contabilidad/conciliacion")
                        .param("fechaDesde", "2026-01-01")
                        .param("fechaHasta", "2026-01-31")
                        .param("direccion", "RECIBIDO")
                        .param("tipoDocumento", "01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coincidencias").value(1))
                .andExpect(jsonPath("$.lineas[0].resultado").value("COINCIDE"));
    }
}
