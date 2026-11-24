package com.magnetixdian.infrastructure.pago;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingEntitlementRepository extends JpaRepository<BillingEntitlementJpa, Long> {

    Optional<BillingEntitlementJpa> findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(Long empresaId, String estado);

    List<BillingEntitlementJpa> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);
}
