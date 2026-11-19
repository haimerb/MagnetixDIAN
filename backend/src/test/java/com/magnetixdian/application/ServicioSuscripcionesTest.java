package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import com.magnetixdian.infrastructure.persistence.PlanJpa;
import com.magnetixdian.infrastructure.persistence.PlanRepository;
import com.magnetixdian.infrastructure.persistence.SuscripcionJpa;
import com.magnetixdian.infrastructure.persistence.SuscripcionRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.SuscripcionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioSuscripcionesTest {

    private PlanRepository planRepo;
    private SuscripcionRepository suscripcionRepo;
    private EmpresaRepository empresaRepo;
    private FacturaRepository facturaRepo;
    private UsuarioRepository usuarioRepo;
    private AuditoriaService auditoria;
    private ServicioSuscripciones servicio;

    @BeforeEach
    void setUp() {
        planRepo = mock(PlanRepository.class);
        suscripcionRepo = mock(SuscripcionRepository.class);
        empresaRepo = mock(EmpresaRepository.class);
        facturaRepo = mock(FacturaRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        auditoria = mock(AuditoriaService.class);
        servicio = new ServicioSuscripciones(planRepo, suscripcionRepo, empresaRepo,
                facturaRepo, usuarioRepo, auditoria);
    }

    private SuscripcionJpa suscripcion(int limiteDocs, int limiteUsuarios) {
        PlanJpa plan = mock(PlanJpa.class);
        when(plan.getLimiteFacturasMes()).thenReturn(limiteDocs);
        when(plan.getLimiteUsuarios()).thenReturn(limiteUsuarios);
        when(plan.getNombre()).thenReturn("Gratis");
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(1L);
        SuscripcionJpa s = mock(SuscripcionJpa.class);
        when(s.getPlan()).thenReturn(plan);
        when(s.getEmpresa()).thenReturn(empresa);
        return s;
    }

    private void conSuscripcionActiva(SuscripcionJpa s) {
        when(suscripcionRepo.findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(eq(1L), eq("ACTIVA")))
                .thenReturn(Optional.of(s));
    }

    @Test
    void sinSuscripcionNoAplicaLimiteDocumentos() {
        when(suscripcionRepo.findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        servicio.verificarLimiteDocumentos(1L);
        verify(facturaRepo, never()).contarEmitidasEntre(anyLong(), any(), any());
    }

    @Test
    void documentosExcedidosLanzaConflicto() {
        conSuscripcionActiva(suscripcion(50, 5));
        when(facturaRepo.contarEmitidasEntre(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(50L);
        assertThrows(IllegalStateException.class, () -> servicio.verificarLimiteDocumentos(1L));
    }

    @Test
    void planIlimitadoNoExcedeDocumentos() {
        conSuscripcionActiva(suscripcion(0, 0));
        when(facturaRepo.contarEmitidasEntre(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(9999L);
        servicio.verificarLimiteDocumentos(1L);
    }

    @Test
    void usuariosExcedidosLanzaConflicto() {
        conSuscripcionActiva(suscripcion(1000, 5));
        when(usuarioRepo.countByEmpresaId(1L)).thenReturn(5L);
        assertThrows(IllegalStateException.class, () -> servicio.verificarLimiteUsuarios(1L));
    }

    @Test
    void cambiarPlanCancelaAnteriorYCreaNueva() {
        EmpresaJpa empresa = mock(EmpresaJpa.class);
        when(empresa.getId()).thenReturn(1L);
        when(empresaRepo.findById(1L)).thenReturn(Optional.of(empresa));
        PlanJpa pro = mock(PlanJpa.class);
        when(pro.getCodigo()).thenReturn("PRO");
        when(pro.getLimiteFacturasMes()).thenReturn(1000);
        when(pro.getLimiteUsuarios()).thenReturn(5);
        when(planRepo.findByCodigoIgnoreCase("PRO")).thenReturn(Optional.of(pro));
        SuscripcionJpa previa = mock(SuscripcionJpa.class);
        when(suscripcionRepo.findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(eq(1L), eq("ACTIVA")))
                .thenReturn(Optional.of(previa));
        when(suscripcionRepo.save(any(SuscripcionJpa.class))).thenAnswer(i -> i.getArgument(0));

        SuscripcionDto dto = servicio.cambiarPlan(1L, "PRO", "wompi");

        assertEquals("PRO", dto.plan().codigo());
        assertEquals("WOMPI", dto.proveedor());
        verify(previa).setEstado("CANCELADA");
        assertNotNull(dto.periodoInicio());
    }

    @Test
    void cambiarPlanInexistenteLanzaError() {
        when(empresaRepo.findById(1L)).thenReturn(Optional.of(mock(EmpresaJpa.class)));
        when(planRepo.findByCodigoIgnoreCase("NADA")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarPlan(1L, "NADA", null));
    }

    @Test
    void sinSuscripcionActivaActualEsNulo() {
        when(suscripcionRepo.findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        assertNull(servicio.actual(1L));
    }

    @Test
    void actualCalculaUsoRestante() {
        SuscripcionJpa s = suscripcion(50, 5);
        conSuscripcionActiva(s);
        when(s.getId()).thenReturn(7L);
        when(s.getEstado()).thenReturn("ACTIVA");
        when(s.getProveedor()).thenReturn("SIMULADO");
        when(s.getPeriodoInicio()).thenReturn(LocalDate.now());
        when(facturaRepo.contarEmitidasEntre(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(10L);
        when(usuarioRepo.countByEmpresaId(1L)).thenReturn(2L);

        SuscripcionDto dto = servicio.actual(1L);

        assertNotNull(dto);
        assertEquals(10, dto.uso().facturasMes());
        assertEquals(40, dto.uso().facturasRestantes());
        assertEquals(3, dto.uso().usuariosRestantes());
    }
}
