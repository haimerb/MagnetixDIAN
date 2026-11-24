package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpRecepcionRepository extends JpaRepository<ErpRecepcionJpa, Long> {
    List<ErpRecepcionJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
    Optional<ErpRecepcionJpa> findByIdAndEmpresaId(Long id, Long empresaId);
}
