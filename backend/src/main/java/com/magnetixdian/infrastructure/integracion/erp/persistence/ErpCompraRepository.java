package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpCompraRepository extends JpaRepository<ErpCompraJpa, Long> {
    List<ErpCompraJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
    Optional<ErpCompraJpa> findByIdAndEmpresaId(Long id, Long empresaId);
}
