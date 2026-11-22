package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentoAsientoRepository extends JpaRepository<DocumentoAsientoJpa, Long> {

    Optional<DocumentoAsientoJpa> findFirstByDocumentoRadianIdOrderByIdDesc(Long documentoRadianId);

    Optional<DocumentoAsientoJpa> findByDocumentoRadianId(Long documentoRadianId);
}
