package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpActivoRepository extends JpaRepository<ErpActivoJpa, Long> {
    List<ErpActivoJpa> findByEmpresaIdOrderByIdDesc(Long empresaId);
    Optional<ErpActivoJpa> findByIdAndEmpresaId(Long id, Long empresaId);
    boolean existsByEmpresaIdAndCodigo(Long empresaId, String codigo);
}
