package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.ResponsabilidadFiscalJpa;
import com.magnetixdian.infrastructure.persistence.ResponsabilidadFiscalRepository;
import com.magnetixdian.interfaces.dto.ResponsabilidadFiscalDto;
import com.magnetixdian.interfaces.dto.SolicitudResponsabilidadFiscalDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ServicioResponsabilidadesFiscales {

    private final ResponsabilidadFiscalRepository repository;

    public ServicioResponsabilidadesFiscales(ResponsabilidadFiscalRepository repository) {
        this.repository = repository;
    }

    public ResponsabilidadFiscalDto registrar(Long empresaId, SolicitudResponsabilidadFiscalDto solicitud) {
        if (solicitud.impuestoCodigo() == null || solicitud.impuestoCodigo().isBlank()) {
            throw new IllegalArgumentException("El código de impuesto es obligatorio.");
        }
        if (solicitud.codigo() == null || solicitud.codigo().isBlank()) {
            throw new IllegalArgumentException("El código de responsabilidad es obligatorio.");
        }
        if (solicitud.desde() == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria.");
        }
        if (solicitud.hasta() != null && solicitud.hasta().isBefore(solicitud.desde())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha de inicio.");
        }

        ResponsabilidadFiscalJpa responsabilidad = new ResponsabilidadFiscalJpa();
        responsabilidad.setEmpresaId(empresaId);
        responsabilidad.setTerceroId(solicitud.terceroId());
        responsabilidad.setImpuestoCodigo(solicitud.impuestoCodigo().trim().toUpperCase(Locale.ROOT));
        responsabilidad.setCodigo(solicitud.codigo().trim().toUpperCase(Locale.ROOT));
        responsabilidad.setDesde(solicitud.desde());
        responsabilidad.setHasta(solicitud.hasta());
        return mapear(repository.save(responsabilidad));
    }

    public List<ResponsabilidadFiscalDto> listar(Long empresaId) {
        return repository.findByEmpresaIdOrderByImpuestoCodigoAscDesdeDesc(empresaId)
                .stream()
                .map(this::mapear)
                .toList();
    }

    public boolean esResponsable(Long empresaId,
                                 String impuestoCodigo,
                                 LocalDate fecha,
                                 Long terceroId) {
        LocalDate fechaEfectiva = fecha == null ? LocalDate.now() : fecha;
        String impuesto = impuestoCodigo == null
                ? null
                : impuestoCodigo.trim().toUpperCase(Locale.ROOT);
        return repository.findByEmpresaIdAndImpuestoCodigo(empresaId, impuesto).stream()
                .filter(responsabilidad -> vigente(responsabilidad, fechaEfectiva))
                .anyMatch(responsabilidad -> responsabilidad.getTerceroId() == null
                        || Objects.equals(responsabilidad.getTerceroId(), terceroId));
    }

    private boolean vigente(ResponsabilidadFiscalJpa responsabilidad, LocalDate fecha) {
        return !fecha.isBefore(responsabilidad.getDesde())
                && (responsabilidad.getHasta() == null || !fecha.isAfter(responsabilidad.getHasta()));
    }

    private ResponsabilidadFiscalDto mapear(ResponsabilidadFiscalJpa responsabilidad) {
        return new ResponsabilidadFiscalDto(
                responsabilidad.getId(),
                responsabilidad.getEmpresaId(),
                responsabilidad.getTerceroId(),
                responsabilidad.getImpuestoCodigo(),
                responsabilidad.getCodigo(),
                responsabilidad.getDesde(),
                responsabilidad.getHasta());
    }
}
