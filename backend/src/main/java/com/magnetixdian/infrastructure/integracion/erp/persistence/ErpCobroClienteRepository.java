package com.magnetixdian.infrastructure.integracion.erp.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ErpCobroClienteRepository extends JpaRepository<ErpCobroClienteJpa, Long> {
    List<ErpCobroClienteJpa> findByEmpresaIdOrderByFechaDescIdDesc(Long empresaId);
    Optional<ErpCobroClienteJpa> findByIdAndEmpresaId(Long id, Long empresaId);
}
