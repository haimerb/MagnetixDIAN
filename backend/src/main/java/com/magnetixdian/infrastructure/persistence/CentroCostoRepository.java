package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CentroCostoRepository extends JpaRepository<CentroCostoJpa, Long> {

    List<CentroCostoJpa> findByEmpresaIdAndActivoTrueOrderByCodigoAsc(Long empresaId);

    Optional<CentroCostoJpa> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
}
