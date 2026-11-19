package com.magnetixdian.application;

import com.magnetixdian.domain.suscripcion.LimiteUso;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import com.magnetixdian.infrastructure.persistence.PlanJpa;
import com.magnetixdian.infrastructure.persistence.PlanRepository;
import com.magnetixdian.infrastructure.persistence.SuscripcionJpa;
import com.magnetixdian.infrastructure.persistence.SuscripcionRepository;
import com.magnetixdian.infrastructure.persistence.UsuarioRepository;
import com.magnetixdian.interfaces.dto.PlanDto;
import com.magnetixdian.interfaces.dto.SuscripcionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Planes y suscripciones de una empresa: consulta, cambio de plan, cancelación
 * y verificación de límites de uso antes de emitir documentos o crear usuarios.
 */
@Service
public class ServicioSuscripciones {

    private final PlanRepository planRepo;
    private final SuscripcionRepository suscripcionRepo;
    private final EmpresaRepository empresaRepo;
    private final FacturaRepository facturaRepo;
    private final UsuarioRepository usuarioRepo;
    private final AuditoriaService auditoria;

    public ServicioSuscripciones(PlanRepository planRepo,
                                 SuscripcionRepository suscripcionRepo,
                                 EmpresaRepository empresaRepo,
                                 FacturaRepository facturaRepo,
                                 UsuarioRepository usuarioRepo,
                                 AuditoriaService auditoria) {
        this.planRepo = planRepo;
        this.suscripcionRepo = suscripcionRepo;
        this.empresaRepo = empresaRepo;
        this.facturaRepo = facturaRepo;
        this.usuarioRepo = usuarioRepo;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<PlanDto> listarPlanes() {
        return planRepo.findByActivoTrueOrderByOrdenAsc().stream().map(this::aPlan).toList();
    }

    @Transactional(readOnly = true)
    public SuscripcionDto actual(Long empresaId) {
        return suscripcionRepo.findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, "ACTIVA")
                .map(this::aDto)
                .orElse(null);
    }

    @Transactional
    public SuscripcionDto cambiarPlan(Long empresaId, String planCodigo, String proveedor) {
        if (planCodigo == null || planCodigo.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el plan.");
        }
        EmpresaJpa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
        PlanJpa plan = planRepo.findByCodigoIgnoreCase(planCodigo)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado: " + planCodigo));

        suscripcionRepo.findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, "ACTIVA")
                .ifPresent(actual -> {
                    actual.setEstado("CANCELADA");
                    actual.setPeriodoFin(LocalDate.now());
                    actual.setUpdatedAt(OffsetDateTime.now());
                    suscripcionRepo.save(actual);
                });

        SuscripcionJpa nueva = new SuscripcionJpa();
        nueva.setEmpresa(empresa);
        nueva.setPlan(plan);
        nueva.setEstado("ACTIVA");
        nueva.setProveedor(normalizarProveedor(proveedor));
        nueva.setPeriodoInicio(LocalDate.now());
        suscripcionRepo.save(nueva);

        auditoria.registrar(AuditoriaService.usuarioActual(), "SUSCRIPCION_CAMBIADA", "suscripcion",
                nueva.getId(), "Plan " + plan.getCodigo() + " proveedor " + nueva.getProveedor());
        return aDto(nueva);
    }

    @Transactional
    public void cancelar(Long empresaId) {
        SuscripcionJpa actual = suscripcionRepo
                .findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, "ACTIVA")
                .orElseThrow(() -> new IllegalStateException("La empresa no tiene una suscripción activa."));
        actual.setEstado("CANCELADA");
        actual.setPeriodoFin(LocalDate.now());
        actual.setUpdatedAt(OffsetDateTime.now());
        suscripcionRepo.save(actual);
        auditoria.registrar(AuditoriaService.usuarioActual(), "SUSCRIPCION_CANCELADA", "suscripcion",
                actual.getId(), "Empresa " + empresaId);
    }

    /**
     * Verifica el límite mensual de documentos. Sin suscripción activa no se aplica límite.
     */
    @Transactional(readOnly = true)
    public void verificarLimiteDocumentos(Long empresaId) {
        SuscripcionJpa s = suscripcionRepo
                .findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, "ACTIVA").orElse(null);
        if (s == null) {
            return;
        }
        int limite = s.getPlan().getLimiteFacturasMes();
        long usados = documentosDelMes(empresaId);
        if (LimiteUso.excedido((int) usados, limite)) {
            throw new IllegalStateException("Alcanzó el límite de " + limite
                    + " documentos del plan " + s.getPlan().getNombre() + ". Actualice su plan para continuar.");
        }
    }

    /**
     * Verifica el límite de usuarios. Sin suscripción activa no se aplica límite.
     */
    @Transactional(readOnly = true)
    public void verificarLimiteUsuarios(Long empresaId) {
        SuscripcionJpa s = suscripcionRepo
                .findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, "ACTIVA").orElse(null);
        if (s == null) {
            return;
        }
        int limite = s.getPlan().getLimiteUsuarios();
        int usados = (int) usuarioRepo.countByEmpresaId(empresaId);
        if (LimiteUso.excedido(usados, limite)) {
            throw new IllegalStateException("Alcanzó el límite de " + limite
                    + " usuarios del plan " + s.getPlan().getNombre() + ". Actualice su plan para continuar.");
        }
    }

    private long documentosDelMes(Long empresaId) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = inicio.plusMonths(1);
        return facturaRepo.contarEmitidasEntre(empresaId, inicio, fin);
    }

    private SuscripcionDto aDto(SuscripcionJpa s) {
        PlanJpa plan = s.getPlan();
        int facturas = (int) documentosDelMes(s.getEmpresa().getId());
        int usuarios = (int) usuarioRepo.countByEmpresaId(s.getEmpresa().getId());
        SuscripcionDto.UsoDto uso = new SuscripcionDto.UsoDto(
                facturas,
                LimiteUso.restante(facturas, plan.getLimiteFacturasMes()),
                LimiteUso.excedido(facturas, plan.getLimiteFacturasMes()),
                usuarios,
                LimiteUso.restante(usuarios, plan.getLimiteUsuarios()),
                LimiteUso.excedido(usuarios, plan.getLimiteUsuarios()));
        return new SuscripcionDto(s.getId(), s.getEmpresa().getId(), aPlan(plan), s.getEstado(),
                s.getProveedor(), s.getReferenciaExterna(), s.getPeriodoInicio(), s.getPeriodoFin(), uso);
    }

    private PlanDto aPlan(PlanJpa p) {
        return new PlanDto(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(),
                p.getPrecioMensual(), p.getMoneda(), p.getLimiteFacturasMes(), p.getLimiteUsuarios(),
                p.getOrden());
    }

    private static String normalizarProveedor(String proveedor) {
        if (proveedor == null || proveedor.isBlank()) {
            return "SIMULADO";
        }
        return proveedor.trim().toUpperCase();
    }
}
