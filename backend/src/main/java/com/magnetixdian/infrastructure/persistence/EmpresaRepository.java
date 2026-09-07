package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<EmpresaJpa, Long> {

    java.util.Optional<EmpresaJpa> findByNit(String nit);
}