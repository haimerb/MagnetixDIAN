package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsientoContableRepository extends JpaRepository<AsientoContableJpa, Long> {

    Optional<AsientoContableJpa> findByEmpresaIdAndDocumentoTipoAndDocumentoIdAndTipoEvento(
            Long empresaId, String documentoTipo, Long documentoId, String tipoEvento);

    List<AsientoContableJpa> findByEmpresaIdAndDocumentoTipo(Long empresaId, String documentoTipo);

    List<AsientoContableJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
}
