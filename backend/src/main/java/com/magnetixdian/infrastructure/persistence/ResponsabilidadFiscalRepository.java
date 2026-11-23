package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponsabilidadFiscalRepository extends JpaRepository<ResponsabilidadFiscalJpa, Long> {

    List<ResponsabilidadFiscalJpa> findByEmpresaIdAndImpuestoCodigo(Long empresaId, String impuestoCodigo);

    List<ResponsabilidadFiscalJpa> findByEmpresaIdOrderByImpuestoCodigoAscDesdeDesc(Long empresaId);
}
