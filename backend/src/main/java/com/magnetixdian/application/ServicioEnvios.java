package com.magnetixdian.application;

import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import com.magnetixdian.infrastructure.persistence.EnvioFacturaJpa;
import com.magnetixdian.infrastructure.persistence.EnvioFacturaRepository;
import com.magnetixdian.infrastructure.persistence.FacturaEventoJpa;
import com.magnetixdian.infrastructure.persistence.FacturaEventoRepository;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import com.magnetixdian.interfaces.dto.EnvioFacturaDto;
import com.magnetixdian.interfaces.dto.PaginaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cola de envíos electrónicos: registra el resultado del intento de emisión y
 * reintenta los documentos rechazados con backoff exponencial.
 */
@Service
public class ServicioEnvios {

    private static final Logger log = LoggerFactory.getLogger(ServicioEnvios.class);

    private static final String PENDIENTE = "PENDIENTE";
    private static final String EXITOSO = "EXITOSO";
    private static final String AGOTADO = "AGOTADO";

    private final EnvioFacturaRepository envioRepository;
    private final FacturaEventoRepository eventoRepository;
    private final EmpresaFeConfigRepository configRepository;
    private final ServicioTransmision transmision;
    private final AuditoriaService auditoria;

    public ServicioEnvios(EnvioFacturaRepository envioRepository,
                          FacturaEventoRepository eventoRepository,
                          EmpresaFeConfigRepository configRepository,
                          ServicioTransmision transmision,
                          AuditoriaService auditoria) {
        this.envioRepository = envioRepository;
        this.eventoRepository = eventoRepository;
        this.configRepository = configRepository;
        this.transmision = transmision;
        this.auditoria = auditoria;
    }

    /**
     * Registra el intento de transmisión realizado durante la emisión.
     */
    @Transactional
    public void registrarIntento(FacturaJpa factura, EmpresaFeConfigJpa config, String cufe,
                                 ResultadoTransmision resultado) {
        EnvioFacturaJpa envio = envioRepository.findByFacturaId(factura.getId())
                .orElseGet(EnvioFacturaJpa::new);
        envio.setFactura(factura);
        envio.setCufe(cufe);
        envio.setCanal(canal(config));
        envio.setIntentos(envio.getIntentos() + 1);
        if (resultado.exitoso()) {
            envio.setEstado(EXITOSO);
            envio.setUltimoError(null);
            envio.setProximoIntento(OffsetDateTime.now());
        } else {
            envio.setEstado(PENDIENTE);
            envio.setUltimoError(recortar(resultado.mensaje(), 500));
            envio.setProximoIntento(OffsetDateTime.now().plusMinutes(
                    BackoffTransmision.minutos(envio.getIntentos())));
        }
        envio.setUpdatedAt(OffsetDateTime.now());
        envioRepository.save(envio);
    }

    @Transactional(readOnly = true)
    public PaginaDto<EnvioFacturaDto> listar(String estado, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<EnvioFacturaJpa> page = estado == null || estado.isBlank()
                ? envioRepository.findAll(pageable)
                : envioRepository.findByEstado(estado.toUpperCase(), pageable);
        return PaginaDto.desde(page.map(this::aDto));
    }

    @Transactional(readOnly = true)
    public List<Long> pendientesParaReintentar() {
        return envioRepository
                .findTop20ByEstadoAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(
                        PENDIENTE, OffsetDateTime.now())
                .stream()
                .map(EnvioFacturaJpa::getId)
                .toList();
    }

    /**
     * Reintenta un envío. Cada reintento corre en su propia transacción para que
     * un fallo no revierta los demás.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnvioFacturaDto reintentar(Long id, boolean manual) {
        EnvioFacturaJpa envio = envioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Envío no encontrado"));
        if (EXITOSO.equals(envio.getEstado())) {
            return aDto(envio);
        }
        if (manual) {
            envio.setIntentos(0);
            envio.setEstado(PENDIENTE);
            auditoria.registrar(usuarioAuditoria(), "FACTURA_REINTENTO_MANUAL", "factura",
                    envio.getFactura().getId(), envio.getFactura().getNumeroCompleto());
        }
        ejecutar(envio);
        envioRepository.save(envio);
        return aDto(envio);
    }

    private void ejecutar(EnvioFacturaJpa envio) {
        FacturaJpa factura = envio.getFactura();
        EmpresaFeConfigJpa config = configRepository.findByEmpresaId(factura.getEmpresa().getId())
                .orElse(null);
        if (config == null || factura.getXmlFirmado() == null || factura.getXmlFirmado().isBlank()) {
            agotar(envio, "No hay XML firmado o configuración de facturación para reintentar");
            return;
        }

        envio.setIntentos(envio.getIntentos() + 1);
        envio.setUpdatedAt(OffsetDateTime.now());
        String cufe = factura.getCufe() != null ? factura.getCufe() : factura.getCude();
        DatosEnvio datos = new DatosEnvio(factura.getNumeroCompleto(), factura.getPrefijo(),
                factura.getNumero(), factura.getAmbiente());

        ResultadoTransmision resultado = transmision.enviar(factura.getXmlFirmado(), cufe, datos, config);
        factura.setEstado(resultado.exitoso() ? "ACEPTADO" : "RECHAZADO");
        factura.setRespuestaDian(resultado.respuestaCruda());
        if (resultado.exitoso()) {
            factura.setFechaValidacion(OffsetDateTime.now());
        }

        FacturaEventoJpa evento = new FacturaEventoJpa();
        evento.setFactura(factura);
        evento.setTipoEvento("REINTENTO");
        evento.setEstado(resultado.exitoso() ? "ACEPTADO" : "RECHAZADO");
        evento.setXml(factura.getXmlFirmado());
        evento.setRespuesta(resultado.respuestaCruda());
        eventoRepository.save(evento);

        if (resultado.exitoso()) {
            envio.setEstado(EXITOSO);
            envio.setUltimoError(null);
            envio.setProximoIntento(OffsetDateTime.now());
            auditoria.registrar(usuarioAuditoria(), "FACTURA_REINTENTO_OK", "factura", factura.getId(),
                    factura.getNumeroCompleto());
        } else {
            envio.setUltimoError(recortar(resultado.mensaje(), 500));
            if (envio.getIntentos() >= envio.getMaxIntentos()) {
                envio.setEstado(AGOTADO);
                envio.setProximoIntento(OffsetDateTime.now());
                auditoria.registrar(usuarioAuditoria(), "FACTURA_REINTENTO_AGOTADO", "factura",
                        factura.getId(), factura.getNumeroCompleto() + ": " + envio.getUltimoError());
            } else {
                envio.setEstado(PENDIENTE);
                envio.setProximoIntento(OffsetDateTime.now().plusMinutes(
                        BackoffTransmision.minutos(envio.getIntentos())));
            }
        }
        log.info("Reintento de envío {} para factura {}: {} (intento {}/{})", envio.getId(),
                factura.getNumeroCompleto(), envio.getEstado(), envio.getIntentos(), envio.getMaxIntentos());
    }

    private void agotar(EnvioFacturaJpa envio, String motivo) {
        envio.setEstado(AGOTADO);
        envio.setUltimoError(recortar(motivo, 500));
        envio.setProximoIntento(OffsetDateTime.now());
        envio.setUpdatedAt(OffsetDateTime.now());
    }

    private EnvioFacturaDto aDto(EnvioFacturaJpa e) {
        return new EnvioFacturaDto(e.getId(), e.getFactura().getId(), e.getFactura().getNumeroCompleto(),
                e.getCufe(), e.getCanal(), e.getEstado(), e.getIntentos(), e.getMaxIntentos(),
                e.getProximoIntento(), e.getUltimoError(), e.getUpdatedAt());
    }

    private static String canal(EmpresaFeConfigJpa config) {
        return config == null || config.getCanal() == null
                ? "SIMULADO" : config.getCanal().toUpperCase();
    }

    static String recortar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }

    private static String usuarioAuditoria() {
        String usuario = AuditoriaService.usuarioActual();
        return usuario == null || "anonymousUser".equals(usuario) ? "sistema" : usuario;
    }
}
