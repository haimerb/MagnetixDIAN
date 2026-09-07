package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedioMagneticoRepository extends JpaRepository<MedioMagneticoJpa, Long> {

    Optional<MedioMagneticoJpa> findByEmpresaIdAndFormatoAndAnioGravable(Long empresaId, String formato, Integer anioGravable);

    List<MedioMagneticoJpa> findByEmpresaIdOrderByCreatedAtDesc(Long empresaId);

    List<MedioMagneticoJpa> findByEstadoIn(List<String> estados);
}