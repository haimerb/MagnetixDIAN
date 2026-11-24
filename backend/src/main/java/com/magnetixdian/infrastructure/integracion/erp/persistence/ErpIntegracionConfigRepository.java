package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpIntegracionConfigRepository extends JpaRepository<ErpIntegracionConfigJpa, Long> {
    List<ErpIntegracionConfigJpa> findByEmpresaIdOrderByProveedorAsc(Long empresaId);
    Optional<ErpIntegracionConfigJpa> findByEmpresaIdAndProveedorAndTipo(Long empresaId, String proveedor, String tipo);
}
