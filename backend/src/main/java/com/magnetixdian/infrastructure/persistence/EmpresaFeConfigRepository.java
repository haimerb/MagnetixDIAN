package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmpresaFeConfigRepository extends JpaRepository<EmpresaFeConfigJpa, Long> {

    Optional<EmpresaFeConfigJpa> findByEmpresaId(Long empresaId);

    @Query("select c from EmpresaFeConfigJpa c join fetch c.empresa where c.empresa.id = :empresaId")
    Optional<EmpresaFeConfigJpa> findConEmpresa(@Param("empresaId") Long empresaId);
}
