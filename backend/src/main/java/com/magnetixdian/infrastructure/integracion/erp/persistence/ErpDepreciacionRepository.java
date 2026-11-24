package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpDepreciacionRepository extends JpaRepository<ErpDepreciacionJpa, Long> {
    Optional<ErpDepreciacionJpa> findByActivoIdAndPeriodo(Long activoId, String periodo);
    List<ErpDepreciacionJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
}
