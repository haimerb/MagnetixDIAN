package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaContableRepository extends JpaRepository<CuentaContableJpa, Long> {

    List<CuentaContableJpa> findByEmpresaId(Long empresaId);

    List<CuentaContableJpa> findByEmpresaIdIsNull();

    Optional<CuentaContableJpa> findByEmpresaIdAndCodigo(Long empresaId, String codigo);

    Optional<CuentaContableJpa> findByEmpresaIdIsNullAndCodigo(String codigo);

    List<CuentaContableJpa> findByEmpresaIdAndVersionPuc(Long empresaId, String versionPuc);

    List<CuentaContableJpa> findByEmpresaIdIsNullAndVersionPuc(String versionPuc);
}
