package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportacionStagingRepository extends JpaRepository<ImportacionStagingJpa, Long> {

    Optional<ImportacionStagingJpa> findByEmpresaIdAndArchivoIdAndTipoAndExternalId(
            Long empresaId, String archivoId, String tipo, String externalId);

    Optional<ImportacionStagingJpa> findByEmpresaIdAndArchivoIdAndTipo(
            Long empresaId, String archivoId, String tipo);

    List<ImportacionStagingJpa> findByEmpresaIdAndArchivoIdAndTipoOrderByFilaAsc(
            Long empresaId, String archivoId, String tipo);
}
