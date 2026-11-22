package com.magnetixdian.application;

import com.magnetixdian.domain.radian.EstadoCausacion;
import com.magnetixdian.infrastructure.persistence.CausacionRadianJpa;
import com.magnetixdian.infrastructure.persistence.CausacionRadianRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianJpa;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.interfaces.dto.ResultadoCausacionDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional
public class ServicioCausacionRadian {

    private final DocumentoRadianRepository documentoRepository;
    private final CausacionRadianRepository causacionRepository;

    public ServicioCausacionRadian(DocumentoRadianRepository documentoRepository,
                                   CausacionRadianRepository causacionRepository) {
        this.documentoRepository = documentoRepository;
        this.causacionRepository = causacionRepository;
    }

    public ResultadoCausacionDto causar(Long empresaId, Long documentoId, SolicitudCausacionDto solicitud) {
        DocumentoRadianJpa documento = documentoRepository.findByIdAndEmpresaId(documentoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Documento RADIAN no encontrado."));

        Optional<CausacionRadianJpa> existente = causacionRepository.findByDocumentoRadianId(documentoId);
        if (existente.isPresent()) {
            return aDto(existente.get());
        }

        BigDecimal total = documento.getTotal() == null
                ? BigDecimal.ZERO : documento.getTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal retenciones = documento.getRetenciones() == null
                ? BigDecimal.ZERO : documento.getRetenciones().setScale(2, RoundingMode.HALF_UP);
        if (retenciones.compareTo(total) > 0) {
            throw new IllegalArgumentException("Las retenciones no pueden superar el total del documento.");
        }

        LocalDate fecha = solicitud == null || solicitud.fechaCausacion() == null
                ? LocalDate.now() : solicitud.fechaCausacion();

        CausacionRadianJpa causacion = new CausacionRadianJpa();
        causacion.setDocumentoRadianId(documentoId);
        causacion.setEmpresaId(empresaId);
        causacion.setFechaCausacion(fecha);
        causacion.setTotal(total);
        if (solicitud != null && solicitud.observaciones() != null) {
            causacion.setObservaciones(solicitud.observaciones().trim());
        }
        causacion.setEstado(EstadoCausacion.CAUSADA);

        CausacionRadianJpa guardada = causacionRepository.saveAndFlush(causacion);
        documento.setEstadoCausacion(EstadoCausacion.CAUSADA);
        documentoRepository.save(documento);
        return aDto(guardada == null ? causacion : guardada);
    }

    private static ResultadoCausacionDto aDto(CausacionRadianJpa causacion) {
        return new ResultadoCausacionDto(
                causacion.getDocumentoRadianId(),
                causacion.getId(),
                causacion.getEstado(),
                causacion.getFechaCausacion(),
                causacion.getTotal(),
                "Causación registrada.");
    }
}
