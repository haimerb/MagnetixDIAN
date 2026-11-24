package com.magnetixdian.infrastructure.pago;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingCheckoutRepository extends JpaRepository<BillingCheckoutJpa, Long> {

    Optional<BillingCheckoutJpa> findByReferencia(String referencia);

    List<BillingCheckoutJpa> findByEmpresaId(Long empresaId);
}
