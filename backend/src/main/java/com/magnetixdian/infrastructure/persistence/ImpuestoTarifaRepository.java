package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImpuestoTarifaRepository extends JpaRepository<ImpuestoTarifaJpa, Long> {

    List<ImpuestoTarifaJpa> findByImpuestoCodigoAndAnioAndActivoTrueOrderByConceptoCodigoAsc(
            String impuestoCodigo, Integer anio);

    Optional<ImpuestoTarifaJpa> findFirstByImpuestoCodigoAndAnioAndActivoTrueAndConceptoCodigoIsNull(
            String impuestoCodigo, Integer anio);

    List<ImpuestoTarifaJpa> findByAnioOrderByImpuestoCodigoAscConceptoCodigoAsc(Integer anio);
}
