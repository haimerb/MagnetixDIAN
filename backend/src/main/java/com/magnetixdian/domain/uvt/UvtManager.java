package com.magnetixdian.domain.uvt;

import com.magnetixdian.infrastructure.persistence.UvtVigenciaJpa;
import com.magnetixdian.infrastructure.persistence.UvtVigenciaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Gestor del valor de la UVT por año gravable.
 *
 * <p>Los valores se cargan desde la tabla {@code uvt_vigencia}, actualizable
 * cada año sin redeploy (se siembran con Flyway y se administran vía API).</p>
 */
@Service
public class UvtManager {

    private final UvtVigenciaRepository repository;

    public UvtManager(UvtVigenciaRepository repository) {
        this.repository = repository;
    }

    public BigDecimal uvtParaAnio(Integer anioGrabeable) {
        return repository.findByAnioAndActivoTrue(anioGrabeable)
                .map(UvtVigenciaJpa::getValor)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe UVT configurada para el año gravable " + anioGrabeable));
    }

    public boolean existeUvt(Integer anioGrabeable) {
        return repository.findByAnioAndActivoTrue(anioGrabeable).isPresent();
    }
}