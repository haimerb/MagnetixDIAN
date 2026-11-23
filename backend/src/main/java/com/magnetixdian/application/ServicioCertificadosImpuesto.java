package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.CertificadoImpuestoJpa;
import com.magnetixdian.infrastructure.persistence.CertificadoImpuestoRepository;
import com.magnetixdian.infrastructure.persistence.TerceroContableRepository;
import com.magnetixdian.interfaces.dto.CertificadoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudCertificadoImpuestoDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ServicioCertificadosImpuesto {

    private final CertificadoImpuestoRepository repository;
    private final TerceroContableRepository terceroRepository;

    public ServicioCertificadosImpuesto(CertificadoImpuestoRepository repository,
                                        TerceroContableRepository terceroRepository) {
        this.repository = repository;
        this.terceroRepository = terceroRepository;
    }

    public CertificadoImpuestoDto emitir(Long empresaId, SolicitudCertificadoImpuestoDto solicitud) {
        if (solicitud.terceroId() == null) {
            throw new IllegalArgumentException("El tercero es obligatorio.");
        }
        if (solicitud.impuestoCodigo() == null || solicitud.impuestoCodigo().isBlank()) {
            throw new IllegalArgumentException("El código de impuesto es obligatorio.");
        }
        if (solicitud.numero() == null || solicitud.numero().isBlank()) {
            throw new IllegalArgumentException("El número del certificado es obligatorio.");
        }
        if (solicitud.valor() == null || solicitud.valor().signum() < 0) {
            throw new IllegalArgumentException("El valor del certificado debe ser mayor o igual a cero.");
        }
        if (solicitud.anio() == null) {
            throw new IllegalArgumentException("El año del certificado es obligatorio.");
        }
        terceroRepository.findById(solicitud.terceroId())
                .filter(tercero -> tercero.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Tercero no encontrado."));

        String impuesto = solicitud.impuestoCodigo().trim().toUpperCase(Locale.ROOT);
        String numero = solicitud.numero().trim();
        var existente = repository.findByEmpresaIdAndTerceroIdAndImpuestoCodigoAndAnioAndNumero(
                empresaId, solicitud.terceroId(), impuesto, solicitud.anio(), numero);
        if (existente.isPresent()) {
            return mapear(existente.get());
        }

        CertificadoImpuestoJpa certificado = new CertificadoImpuestoJpa();
        certificado.setEmpresaId(empresaId);
        certificado.setTerceroId(solicitud.terceroId());
        certificado.setImpuestoCodigo(impuesto);
        certificado.setAnio(solicitud.anio());
        certificado.setNumero(numero);
        certificado.setValor(solicitud.valor());
        certificado.setFechaEmision(solicitud.fechaEmision() == null
                ? LocalDate.now()
                : solicitud.fechaEmision());
        return mapear(repository.save(certificado));
    }

    public List<CertificadoImpuestoDto> listar(Long empresaId, Integer anio) {
        int anioEfectivo = anio == null ? LocalDate.now().getYear() : anio;
        return repository.findByEmpresaIdAndAnioOrderByFechaEmisionDescIdDesc(empresaId, anioEfectivo)
                .stream()
                .map(this::mapear)
                .toList();
    }

    private CertificadoImpuestoDto mapear(CertificadoImpuestoJpa certificado) {
        return new CertificadoImpuestoDto(
                certificado.getId(),
                certificado.getEmpresaId(),
                certificado.getTerceroId(),
                certificado.getImpuestoCodigo(),
                certificado.getAnio(),
                certificado.getNumero(),
                certificado.getValor(),
                certificado.getFechaEmision());
    }
}
