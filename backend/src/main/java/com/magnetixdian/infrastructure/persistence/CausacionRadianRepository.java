package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CausacionRadianRepository extends JpaRepository<CausacionRadianJpa, Long> {

    Optional<CausacionRadianJpa> findByDocumentoRadianId(Long documentoRadianId);

    Optional<CausacionRadianJpa> findByIdAndEmpresaId(Long id, Long empresaId);
}
