package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpPagoProveedorRepository extends JpaRepository<ErpPagoProveedorJpa, Long> {
    List<ErpPagoProveedorJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
    Optional<ErpPagoProveedorJpa> findByIdAndEmpresaId(Long id, Long empresaId);
}
