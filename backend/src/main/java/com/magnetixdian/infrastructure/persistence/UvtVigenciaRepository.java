package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UvtVigenciaRepository extends JpaRepository<UvtVigenciaJpa, Integer> {

    Optional<UvtVigenciaJpa> findByAnioAndActivoTrue(Integer anio);
}