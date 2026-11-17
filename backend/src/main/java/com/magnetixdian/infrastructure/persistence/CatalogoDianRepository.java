package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogoDianRepository extends JpaRepository<CatalogoDianJpa, Long> {

    List<CatalogoDianJpa> findByTipoAndActivoTrueOrderByCodigo(String tipo);

    List<CatalogoDianJpa> findByTipoOrderByCodigo(String tipo);
}
