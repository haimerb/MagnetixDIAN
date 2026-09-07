package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValidacionRepository extends JpaRepository<ValidacionJpa, Long> {

    Optional<ValidacionJpa> findTopByMedioMagneticoIdOrderByInicioDesc(Long medioMagneticoId);

    List<ValidacionJpa> findByMedioMagneticoIdOrderByInicioDesc(Long medioMagneticoId);
}