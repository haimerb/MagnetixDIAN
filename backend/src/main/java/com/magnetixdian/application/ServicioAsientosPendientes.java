package com.magnetixdian.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.infrastructure.persistence.AsientoPendienteJpa;
import com.magnetixdian.infrastructure.persistence.AsientoPendienteRepository;
import com.magnetixdian.interfaces.dto.SolicitudAsientoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ServicioAsientosPendientes {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String PROCESADO = "PROCESADO";
    public static final String FALLIDO = "FALLIDO";

    private static final int MAX_INTENTOS = 5;
    private static final Logger log = LoggerFactory.getLogger(ServicioAsientosPendientes.class);
    private static final List<String> ESTADOS_REGISTRADOS = List.of(PENDIENTE, PROCESADO, FALLIDO);

    private final AsientoPendienteRepository repository;
    private final ServicioAsientos servicioAsientos;
    private final ObjectMapper objectMapper;

    public ServicioAsientosPendientes(AsientoPendienteRepository repository,
                                      ServicioAsientos servicioAsientos,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.servicioAsientos = servicioAsientos;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long empresaId, SolicitudAsientoDto solicitud, String error) {
        if (solicitud == null || solicitud.origen() == null || solicitud.origenId() == null) {
            return;
        }
        String tipoEvento = solicitud.tipoEvento() == null || solicitud.tipoEvento().isBlank()
                ? "AJUSTE" : solicitud.tipoEvento();
        boolean existe = repository.existsByEmpresaIdAndOrigenAndOrigenIdAndTipoEventoAndEstadoIn(
                empresaId, solicitud.origen(), solicitud.origenId(), tipoEvento, ESTADOS_REGISTRADOS);
        if (existe) {
            return;
        }
        AsientoPendienteJpa pendiente = new AsientoPendienteJpa();
        pendiente.setEmpresaId(empresaId);
        pendiente.setOrigen(solicitud.origen());
        pendiente.setOrigenId(solicitud.origenId());
        pendiente.setTipoEvento(tipoEvento);
        pendiente.setFecha(solicitud.fecha());
        pendiente.setPayload(serializar(solicitud));
        pendiente.setEstado(PENDIENTE);
        pendiente.setIntentos(0);
        pendiente.setUltimoError(recortar(error));
        repository.save(pendiente);
    }

    @Transactional(readOnly = true)
    public List<AsientoPendienteJpa> listarPendientes() {
        return repository.findByEstadoOrderByCreatedAtAsc(PENDIENTE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean procesar(AsientoPendienteJpa pendiente) {
        try {
            SolicitudAsientoDto solicitud = objectMapper.readValue(pendiente.getPayload(), SolicitudAsientoDto.class);
            servicioAsientos.crear(pendiente.getEmpresaId(), solicitud);
            pendiente.setEstado(PROCESADO);
            pendiente.setUltimoError(null);
        } catch (Exception ex) {
            int intentos = pendiente.getIntentos() + 1;
            pendiente.setIntentos(intentos);
            pendiente.setUltimoError(recortar(ex.getMessage()));
            if (intentos >= MAX_INTENTOS) {
                pendiente.setEstado(FALLIDO);
            }
            log.warn("Reintento {} de asiento pendiente {} {}: {}", intentos, pendiente.getOrigen(),
                    pendiente.getOrigenId(), ex.getMessage());
        }
        pendiente.setUpdatedAt(OffsetDateTime.now());
        repository.save(pendiente);
        return PROCESADO.equals(pendiente.getEstado());
    }

    @Scheduled(fixedDelayString = "${magnetixdian.asientos-pendientes.interval-ms:60000}")
    public void procesarPendientes() {
        for (AsientoPendienteJpa pendiente : listarPendientes()) {
            procesar(pendiente);
        }
    }

    private String serializar(SolicitudAsientoDto solicitud) {
        try {
            return objectMapper.writeValueAsString(solicitud);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo serializar el asiento pendiente.", ex);
        }
    }

    private static String recortar(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        return mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
    }
}
