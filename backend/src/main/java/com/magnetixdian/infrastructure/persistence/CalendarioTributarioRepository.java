package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendarioTributarioRepository extends JpaRepository<CalendarioTributarioJpa, Long> {

    Optional<CalendarioTributarioJpa> findFirstByImpuestoCodigoAndAnioGravableAndPeriodo(
            String impuestoCodigo, Integer anioGravable, String periodo);

    List<CalendarioTributarioJpa> findByAnioGravableOrderByImpuestoCodigoAscPeriodoAsc(Integer anioGravable);
}
