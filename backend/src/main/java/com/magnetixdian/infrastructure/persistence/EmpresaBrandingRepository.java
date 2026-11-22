package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpresaBrandingRepository extends JpaRepository<EmpresaBrandingJpa, Long> {

    Optional<EmpresaBrandingJpa> findByEmpresaId(Long empresaId);

    Optional<EmpresaBrandingJpa> findByPublicSlug(String publicSlug);

    boolean existsByPublicSlug(String publicSlug);
}
