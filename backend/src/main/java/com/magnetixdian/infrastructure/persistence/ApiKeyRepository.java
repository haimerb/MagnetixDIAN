package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKeyJpa, Long> {

    List<ApiKeyJpa> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);

    Optional<ApiKeyJpa> findFirstByPrefijoAndActivoTrue(String prefijo);

    Optional<ApiKeyJpa> findByIdAndEmpresaId(Long id, Long empresaId);
}
