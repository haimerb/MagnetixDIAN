package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuscripcionRepository extends JpaRepository<SuscripcionJpa, Long> {

    Optional<SuscripcionJpa> findFirstByEmpresaIdAndEstadoOrderByCreatedAtDesc(Long empresaId, String estado);

    Optional<SuscripcionJpa> findFirstByEmpresaIdOrderByCreatedAtDesc(Long empresaId);
}
