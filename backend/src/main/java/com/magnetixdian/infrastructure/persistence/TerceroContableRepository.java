package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerceroContableRepository extends JpaRepository<TerceroContableJpa, Long> {

    Optional<TerceroContableJpa> findByEmpresaIdAndTipoDocumentoAndNumeroDocumento(
            Long empresaId, String tipoDocumento, String numeroDocumento);

    List<TerceroContableJpa> findByEmpresaIdAndActivoTrueOrderByNombreAsc(Long empresaId);
}
