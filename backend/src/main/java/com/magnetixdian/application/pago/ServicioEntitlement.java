package com.magnetixdian.application.pago;

import com.magnetixdian.application.ServicioSuscripciones;
import com.magnetixdian.domain.pago.EstadoEntitlement;
import com.magnetixdian.infrastructure.pago.BillingEntitlementJpa;
import com.magnetixdian.infrastructure.pago.BillingEntitlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class ServicioEntitlement {

    private final BillingEntitlementRepository repositorio;
    private final ServicioSuscripciones suscripciones;

    public ServicioEntitlement(BillingEntitlementRepository repositorio, ServicioSuscripciones suscripciones) {
        this.repositorio = repositorio;
        this.suscripciones = suscripciones;
    }

    @Transactional
    public EntitlementDto activar(Long empresaId, String planCode, String proveedor, String referencia) {
        validar(empresaId, planCode);
        LocalDate hoy = LocalDate.now();
        repositorio.findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, EstadoEntitlement.ACTIVA.name())
                .ifPresent(anterior -> {
                    anterior.setEstado(EstadoEntitlement.CANCELADA.name());
                    anterior.setUpdatedAt(OffsetDateTime.now());
                    repositorio.save(anterior);
                });
        BillingEntitlementJpa entitlement = nueva(empresaId, planCode, EstadoEntitlement.ACTIVA, hoy, hoy.plusMonths(1));
        entitlement.setProveedor(proveedor);
        entitlement.setReferencia(referencia);
        suscripciones.cambiarPlan(empresaId, planCode, proveedor);
        return aDto(repositorio.save(entitlement));
    }

    @Transactional
    public EntitlementDto renovar(Long empresaId, String planCode, int dias) {
        validar(empresaId, planCode);
        if (dias <= 0) {
            throw new IllegalArgumentException("La renovación debe indicar días válidos.");
        }
        LocalDate hoy = LocalDate.now();
        LocalDate base = repositorio
                .findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, EstadoEntitlement.ACTIVA.name())
                .map(BillingEntitlementJpa::getFechaExpiracion)
                .filter(fecha -> fecha.isAfter(hoy))
                .orElse(hoy);
        BillingEntitlementJpa actual = repositorio
                .findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, EstadoEntitlement.ACTIVA.name())
                .orElseGet(() -> nueva(empresaId, planCode, EstadoEntitlement.ACTIVA, hoy, base.plusDays(dias)));
        actual.setPlanCode(planCode);
        actual.setEstado(EstadoEntitlement.ACTIVA.name());
        actual.setFechaInicio(hoy);
        actual.setFechaExpiracion(base.plusDays(dias));
        actual.setUpdatedAt(OffsetDateTime.now());
        return aDto(repositorio.save(actual));
    }

    @Transactional
    public EntitlementDto suspender(Long empresaId, String motivo) {
        return cambiarEstado(empresaId, EstadoEntitlement.SUSPENDIDA, motivo);
    }

    @Transactional
    public EntitlementDto cancelar(Long empresaId, String motivo) {
        return cambiarEstado(empresaId, EstadoEntitlement.CANCELADA, motivo);
    }

    @Transactional(readOnly = true)
    public EntitlementDto actual(Long empresaId) {
        Objects.requireNonNull(empresaId, "La empresa es obligatoria.");
        return repositorio
                .findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, EstadoEntitlement.ACTIVA.name())
                .map(e -> aDto(e))
                .orElse(null);
    }

    private EntitlementDto cambiarEstado(Long empresaId, EstadoEntitlement estado, String motivo) {
        Objects.requireNonNull(empresaId, "La empresa es obligatoria.");
        BillingEntitlementJpa entitlement = repositorio
                .findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(empresaId, EstadoEntitlement.ACTIVA.name())
                .orElseThrow(() -> new IllegalStateException("La empresa no tiene un entitlement activo."));
        entitlement.setEstado(estado.name());
        entitlement.setUpdatedAt(OffsetDateTime.now());
        if (motivo != null && !motivo.isBlank()) {
            entitlement.setReferencia(motivo.length() > 120 ? motivo.substring(0, 120) : motivo);
        }
        return aDto(repositorio.save(entitlement));
    }

    private static BillingEntitlementJpa nueva(Long empresaId, String planCode, EstadoEntitlement estado,
                                               LocalDate inicio, LocalDate expiracion) {
        BillingEntitlementJpa entitlement = new BillingEntitlementJpa();
        entitlement.setEmpresaId(empresaId);
        entitlement.setPlanCode(planCode);
        entitlement.setEstado(estado.name());
        entitlement.setFechaInicio(inicio);
        entitlement.setFechaExpiracion(expiracion);
        return entitlement;
    }

    private static void validar(Long empresaId, String planCode) {
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        if (planCode == null || planCode.isBlank()) {
            throw new IllegalArgumentException("El plan es obligatorio.");
        }
    }

    private static EntitlementDto aDto(BillingEntitlementJpa e) {
        return new EntitlementDto(e.getEmpresaId(), e.getPlanCode(), e.getEstado(), e.getFechaInicio(),
                e.getFechaExpiracion(), e.getProveedor(), e.getReferencia());
    }
}
