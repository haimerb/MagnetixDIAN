package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeclaracionImpuestoRepository extends JpaRepository<DeclaracionImpuestoJpa, Long> {

    Optional<DeclaracionImpuestoJpa> findByEmpresaIdAndImpuestoCodigoAndAnioGravableAndPeriodo(
            Long empresaId, String impuestoCodigo, Integer anioGravable, String periodo);

    List<DeclaracionImpuestoJpa> findByEmpresaIdAndAnioGravableOrderByImpuestoCodigoAscPeriodoAsc(
            Long empresaId, Integer anioGravable);
}
