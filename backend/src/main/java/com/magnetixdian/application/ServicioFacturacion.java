package com.magnetixdian.application;

import com.magnetixdian.application.erp.ServicioVentas;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magnetixdian.domain.cufe.CufeCalculator;
import com.magnetixdian.domain.cufe.QrUrlGenerator;
import com.magnetixdian.domain.factura.DatosAdquirente;
import com.magnetixdian.domain.factura.DatosEmisor;
import com.magnetixdian.domain.factura.EventoDian;
import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.factura.ImpuestoFactura;
import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.factura.TipoDocumento;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;
import com.magnetixdian.domain.rule.factura.ContextoFactura;
import com.magnetixdian.domain.rule.factura.ValidadorFactura;
import com.magnetixdian.domain.transmision.DatosEnvio;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.notification.NotificadorEmail;
import com.magnetixdian.infrastructure.pdf.GeneradorPdfFactura;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.DocumentoRadianRepository;
import com.magnetixdian.infrastructure.persistence.ImpuestoPagoRepository;
import com.magnetixdian.infrastructure.persistence.PagoRepository;
import com.magnetixdian.infrastructure.persistence.TerceroContableRepository;
import com.magnetixdian.infrastructure.persistence.FacturaEventoJpa;
import com.magnetixdian.infrastructure.persistence.FacturaEventoRepository;
import com.magnetixdian.infrastructure.persistence.FacturaImpuestoJpa;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaLineaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import com.magnetixdian.infrastructure.xml.GeneradorApplicationResponse;
import com.magnetixdian.infrastructure.xml.GeneradorAttachedDocument;
import com.magnetixdian.infrastructure.xml.GeneradorUblFactura;
import com.magnetixdian.interfaces.dto.AdquirenteRequest;
import com.magnetixdian.interfaces.dto.ErrorValidacionDto;
import com.magnetixdian.interfaces.dto.FacturaEmitidaEvent;
import com.magnetixdian.interfaces.dto.FacturaDetalleDto;
import com.magnetixdian.interfaces.dto.FacturaEventoDto;
import com.magnetixdian.interfaces.dto.FacturaRequest;
import com.magnetixdian.interfaces.dto.FacturaResumenDto;
import com.magnetixdian.interfaces.dto.LineaFacturaRequest;
import com.magnetixdian.interfaces.dto.MovimientoTributarioDto;
import com.magnetixdian.interfaces.dto.RetencionDianDto;
import com.magnetixdian.interfaces.dto.SolicitudEmisionDianDto;
import com.magnetixdian.interfaces.dto.BrandingDto;
import com.magnetixdian.interfaces.dto.PaginaDto;
import com.magnetixdian.interfaces.dto.ResultadoEmisionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio principal de facturación electrónica: valida, numera, genera el UBL,
 * firma, calcula el CUFE/CUDE, produce el PDF/QR, transmite y persiste el
 * documento.
 */
@Service
public class ServicioFacturacion {

    private static final Logger log = LoggerFactory.getLogger(ServicioFacturacion.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern PATRON_TRACK = Pattern.compile(
            "\"(?:trackId|track_id|TrackId|zipKey|ZipKey|ZIPKey)\"\\s*:\\s*\"([^\"]+)\"");

    private static final String COD_IVA = CufeCalculator.IVA;
    private static final String COD_INC = CufeCalculator.INC;

    private final EmpresaRepository empresaRepository;
    private final EmpresaFeConfigRepository configRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaEventoRepository eventoRepository;
    private final ServicioNumeracion numeracion;
    private final ValidadorFactura validador;
    private final GeneradorUblFactura generadorUbl;
    private final GeneradorAttachedDocument generadorAttached;
    private final GeneradorPdfFactura generadorPdf;
    private final ServicioFirma firma;
    private final ServicioTransmision transmision;
    private final NotificadorEmail notificador;
    private final GeneradorApplicationResponse generadorEvento;
    private final AuditoriaService auditoria;
    private final ServicioEnvios envios;
    private final ServicioSuscripciones suscripciones;
    private final ServicioBranding branding;
    private final List<HookEntregaFactura> hooksEntrega;
    private final DocumentoRadianRepository documentoRadianRepository;
    private final PagoRepository pagoRepository;
    private final ImpuestoPagoRepository impuestoPagoRepository;
    private final TerceroContableRepository terceroRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ServicioVentas ventas;

    @FunctionalInterface
    public interface HookEntregaFactura {
        void alEmitir(Long empresaId, Long facturaId, String numeroCompleto);
    }

    public ServicioFacturacion(EmpresaRepository empresaRepository,
                               EmpresaFeConfigRepository configRepository,
                               FacturaRepository facturaRepository,
                               FacturaEventoRepository eventoRepository,
                               ServicioNumeracion numeracion,
                               ValidadorFactura validador,
                               GeneradorUblFactura generadorUbl,
                               GeneradorAttachedDocument generadorAttached,
                               GeneradorPdfFactura generadorPdf,
                               ServicioFirma firma,
                               ServicioTransmision transmision,
                               NotificadorEmail notificador,
                               GeneradorApplicationResponse generadorEvento,
                               AuditoriaService auditoria,
                               ServicioEnvios envios,
                               ServicioSuscripciones suscripciones) {
        this(empresaRepository, configRepository, facturaRepository, eventoRepository, numeracion, validador,
                generadorUbl, generadorAttached, generadorPdf, firma, transmision, notificador, generadorEvento,
                auditoria, envios, suscripciones, null, List.of());
    }

    public ServicioFacturacion(EmpresaRepository empresaRepository,
                               EmpresaFeConfigRepository configRepository,
                               FacturaRepository facturaRepository,
                               FacturaEventoRepository eventoRepository,
                               ServicioNumeracion numeracion,
                               ValidadorFactura validador,
                               GeneradorUblFactura generadorUbl,
                               GeneradorAttachedDocument generadorAttached,
                               GeneradorPdfFactura generadorPdf,
                               ServicioFirma firma,
                               ServicioTransmision transmision,
                               NotificadorEmail notificador,
                               GeneradorApplicationResponse generadorEvento,
                               AuditoriaService auditoria,
                               ServicioEnvios envios,
                               ServicioSuscripciones suscripciones,
                               ServicioBranding branding,
                               List<HookEntregaFactura> hooksEntrega) {
        this(empresaRepository, configRepository, facturaRepository, eventoRepository, numeracion, validador,
                generadorUbl, generadorAttached, generadorPdf, firma, transmision, notificador, generadorEvento,
                auditoria, envios, suscripciones, branding, hooksEntrega, null, null, null, null, null, null);
    }

    @Autowired
    public ServicioFacturacion(EmpresaRepository empresaRepository,
                               EmpresaFeConfigRepository configRepository,
                               FacturaRepository facturaRepository,
                               FacturaEventoRepository eventoRepository,
                               ServicioNumeracion numeracion,
                               ValidadorFactura validador,
                               GeneradorUblFactura generadorUbl,
                               GeneradorAttachedDocument generadorAttached,
                               GeneradorPdfFactura generadorPdf,
                               ServicioFirma firma,
                               ServicioTransmision transmision,
                               NotificadorEmail notificador,
                               GeneradorApplicationResponse generadorEvento,
                               AuditoriaService auditoria,
                               ServicioEnvios envios,
                               ServicioSuscripciones suscripciones,
                               ServicioBranding branding,
                               List<HookEntregaFactura> hooksEntrega,
                               DocumentoRadianRepository documentoRadianRepository,
                               PagoRepository pagoRepository,
                               ImpuestoPagoRepository impuestoPagoRepository,
                                TerceroContableRepository terceroRepository,
                                ApplicationEventPublisher eventPublisher,
                                ServicioVentas ventas) {

        this.empresaRepository = empresaRepository;
        this.configRepository = configRepository;
        this.facturaRepository = facturaRepository;
        this.eventoRepository = eventoRepository;
        this.numeracion = numeracion;
        this.validador = validador;
        this.generadorUbl = generadorUbl;
        this.generadorAttached = generadorAttached;
        this.generadorPdf = generadorPdf;
        this.firma = firma;
        this.transmision = transmision;
        this.notificador = notificador;
        this.generadorEvento = generadorEvento;
        this.auditoria = auditoria;
        this.envios = envios;
        this.suscripciones = suscripciones;
        this.branding = branding;
        this.hooksEntrega = hooksEntrega == null ? List.of() : hooksEntrega;
        this.documentoRadianRepository = documentoRadianRepository;
        this.pagoRepository = pagoRepository;
        this.impuestoPagoRepository = impuestoPagoRepository;
        this.terceroRepository = terceroRepository;
        this.eventPublisher = eventPublisher;
        this.ventas = ventas;
    }

    public List<ErrorValidacionDto> validar(Long empresaId, FacturaRequest req) {
        EmpresaJpa empresa = empresa(empresaId);
        EmpresaFeConfigJpa config = config(empresaId);
        FacturaDatos datos = construirDatos(empresa, config, req, 0L, "");
        return validador.validar(new ContextoFactura(datos, config)).stream()
                .filter(x -> !x.valida())
                .map(this::aError)
                .toList();
    }

    public ResultadoEmisionDto emitir(Long empresaId, Long usuarioId, FacturaRequest req) {
        return emitirSolicitud(empresaId, usuarioId, SolicitudEmisionDianDto.desde(req));
    }

    @Transactional
    public ResultadoEmisionDto emitirSolicitud(Long empresaId, Long usuarioId, SolicitudEmisionDianDto req) {
        if (req == null) {
            throw new IllegalArgumentException("La solicitud de emisión es obligatoria.");
        }
        suscripciones.verificarLimiteDocumentos(empresaId);
        EmpresaJpa empresa = empresa(empresaId);
        EmpresaFeConfigJpa config = config(empresaId);
        TipoDocumento tipo = TipoDocumento.porCodigo(req.tipoDocumento());

        FacturaDatos preliminar = construirDatos(empresa, config, req, 0L, "");
        List<ResultadoRegla> resultados = validador.validar(new ContextoFactura(preliminar, config));
        if (validador.tieneErrores(resultados)) {
            auditoria.registrar(AuditoriaService.usuarioActual(), "FACTURA_RECHAZADA_VALIDACION", "empresa",
                    empresaId, req.tipoDocumento());
            return new ResultadoEmisionDto(false, null, null, null, "RECHAZADA", null,
                    "El documento tiene errores de validación", resultados.stream().filter(x -> !x.valida())
                    .map(this::aError).toList());
        }

        long numero = numeracion.siguiente(empresaId, nvl(config.getPrefijo()), tipo.getCodigo());
        String numeroCompleto = nvl(config.getPrefijo()) + numero;
        FacturaDatos datos = construirDatos(empresa, config, req, numero, numeroCompleto);

        String cufe = CufeCalculator.calcular(datos);
        String qrUrl = QrUrlGenerator.generar(cufe, datos.ambiente());
        String xml = "31".equals(req.tipoDocumento())
                ? generadorUbl.generarNomina(datos, req.nomina(), cufe)
                : generadorUbl.generar(datos, cufe);
        String xmlFirmado = firma.firmar(xml, config);

        ResultadoTransmision transmisionResultado = transmision.enviar(xmlFirmado, cufe, DatosEnvio.de(datos), config);
        String trackId = extraerTrackId(transmisionResultado.respuestaCruda(), cufe);

        FacturaJpa factura = persistir(empresa, usuarioId, config, datos, cufe, qrUrl, xml, xmlFirmado,
                transmisionResultado, req.tipoDocumento());
        guardarPdf(factura, datos, cufe, qrUrl);

        if (transmisionResultado.exitoso() && req.pedidoId() != null) {
            if (ventas == null) {
                throw new IllegalStateException("La correlación con pedidos ERP no está disponible.");
            }
            ventas.vincularFacturaDian(empresaId, req.pedidoId(), numeroCompleto);
        }

        FacturaEventoJpa evento = new FacturaEventoJpa();
        evento.setFactura(factura);
        evento.setTipoEvento("EMISION");
        evento.setEstado(transmisionResultado.exitoso() ? "ACEPTADO" : "RECHAZADO");
        evento.setXml(xmlFirmado);
        evento.setRespuesta("trackId=" + trackId + ";" + nvl(transmisionResultado.respuestaCruda()));
        eventoRepository.save(evento);
        envios.registrarIntento(factura, config, cufe, transmisionResultado);

        String motivo = transmisionResultado.exitoso() ? null : motivoTransmision(transmisionResultado);
        publicarEvento(factura, datos, trackId, motivo);

        if (transmisionResultado.exitoso()) {
            notificarAdquirente(empresa, config, factura, datos, xmlFirmado, numeroCompleto);
            auditoria.registrar(AuditoriaService.usuarioActual(), "FACTURA_EMITIDA", "factura", factura.getId(),
                    numeroCompleto + " · " + factura.getEstado());
            notificarEntrega(empresaId, factura.getId(), numeroCompleto);
        } else {
            auditoria.registrar(AuditoriaService.usuarioActual(), "FACTURA_RECHAZADA_TRANSMISION", "factura",
                    factura.getId(), numeroCompleto + " · " + motivo);
        }

        return new ResultadoEmisionDto(transmisionResultado.exitoso(), factura.getId(), numeroCompleto, cufe,
                factura.getEstado(), qrUrl, transmisionResultado.exitoso() ? transmisionResultado.mensaje() : motivo,
                List.of());
    }

    @Transactional(readOnly = true)
    public List<MovimientoTributarioDto> movimientosParaExogena(Long empresaId, Integer ano) {
        if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria.");
        }
        int anio = ano == null ? LocalDate.now().getYear() : ano;
        LocalDate desde = LocalDate.of(anio, 1, 1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);
        List<MovimientoTributarioDto> movimientos = new ArrayList<>();

        for (FacturaJpa factura : facturaRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId)) {
            LocalDate fecha = factura.getFechaEmision();
            if (fecha == null || fecha.isBefore(desde) || fecha.isAfter(hasta) ||estaRechazada(factura.getEstado())) {
                continue;
            }
            String tipo = nvl(factura.getTipoDocumento());
            String concepto = "31".equals(tipo) ? "NOMINA" : "FACTURA";
            movimientos.add(new MovimientoTributarioDto(empresaId, terceroId(empresaId,
                    factura.getAdquirenteTipoDoc(), factura.getAdquirenteNumDoc()), concepto,
                    nz(factura.getTotal()), fecha, tipo));
            if (factura.getImpuestos() != null) {
                for (FacturaImpuestoJpa impuesto : factura.getImpuestos()) {
                    if (esRetencion(impuesto.getTipo())) {
                        movimientos.add(new MovimientoTributarioDto(empresaId, terceroId(empresaId,
                                factura.getAdquirenteTipoDoc(), factura.getAdquirenteNumDoc()), "RETENCION",
                                nz(impuesto.getValor()), fecha, impuesto.getTipo()));
                    }
                }
            }
        }

        if (documentoRadianRepository != null) {
            for (var documento : documentoRadianRepository.findByEmpresaIdOrderByFechaDescIdDesc(empresaId)) {
                LocalDate fecha = documento.getFecha();
                if (fecha == null || fecha.isBefore(desde) || fecha.isAfter(hasta)) {
                    continue;
                }
                if ("RECIBIDO".equalsIgnoreCase(nvl(documento.getDireccion()))) {
                    movimientos.add(new MovimientoTributarioDto(empresaId, null, "COMPRA",
                            nz(documento.getTotal()), fecha, nvl(documento.getTipoDocumento())));
                }
                if (nz(documento.getRetenciones()).signum() > 0) {
                    movimientos.add(new MovimientoTributarioDto(empresaId, null, "RETENCION",
                            nz(documento.getRetenciones()), fecha, nvl(documento.getTipoDocumento())));
                }
            }
        }

        if (pagoRepository != null) {
            for (var pago : pagoRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId)) {
                LocalDate fecha = pago.getCreatedAt() == null ? null : pago.getCreatedAt().toLocalDate();
                if (fecha == null || fecha.isBefore(desde) || fecha.isAfter(hasta)) {
                    continue;
                }
                movimientos.add(new MovimientoTributarioDto(empresaId, null, "PAGO", nz(pago.getMonto()),
                        fecha, nvl(pago.getReferencia())));
            }
        }

        if (impuestoPagoRepository != null) {
            for (String codigo : new String[]{"01", "04", "05", "06", "07"}) {
                BigDecimal valor = impuestoPagoRepository.sumarPorEmpresaImpuestoPeriodo(empresaId, codigo,
                        desde, hasta);
                if (valor != null && valor.signum() > 0) {
                    movimientos.add(new MovimientoTributarioDto(empresaId, null, "PAGO_IMPUESTO", valor,
                            hasta, codigo));
                }
            }
        }
        return movimientos;
    }

    public void notificarEntrega(Long empresaId, Long facturaId, String numeroCompleto) {
        for (HookEntregaFactura hook : hooksEntrega) {
            try {
                hook.alEmitir(empresaId, facturaId, numeroCompleto);
            } catch (RuntimeException ex) {
                log.warn("Hook de entrega falló para la factura {}: {}", numeroCompleto, ex.getMessage());
            }
        }
    }

    private void notificarAdquirente(EmpresaJpa empresa, EmpresaFeConfigJpa config, FacturaJpa factura,
                                     FacturaDatos datos, String xmlFirmado, String numeroCompleto) {
        if (!config.isNotificarAdquirente()) {
            return;
        }
        String destino = datos.adquirente().email();
        if (destino == null || destino.isBlank()) {
            log.warn("La empresa {} notifica al adquirente pero la factura {} no tiene correo del adquirente",
                    empresa.getId(), numeroCompleto);
            return;
        }
        try {
            byte[] pdf = pdf(empresa.getId(), factura.getId());
            String cufe = factura.getCufe() != null ? factura.getCufe() : factura.getCude();
            String asunto = "Factura electrónica " + numeroCompleto + " - " + empresa.getRazonSocial();
            String cuerpo = "Estimado(a) " + datos.adquirente().nombre() + ",\n\n"
                    + "Adjuntamos la factura electrónica " + numeroCompleto + " emitida por "
                    + empresa.getRazonSocial() + ".\n\nCUFE: " + cufe
                    + "\n\nEste mensaje fue generado automáticamente.";
            notificador.enviarFactura(destino, asunto, cuerpo,
                    "factura-" + numeroCompleto + ".xml", xmlFirmado.getBytes(StandardCharsets.UTF_8),
                    "factura-" + numeroCompleto + ".pdf", pdf);
        } catch (Exception ex) {
            log.warn("No se pudo enviar por correo la factura {}: {}", numeroCompleto, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<FacturaResumenDto> listar(Long empresaId) {
        return facturaRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId).stream()
                .map(this::aResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaDto<FacturaResumenDto> listarPaginado(Long empresaId, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), acotarTamano(tamano),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FacturaResumenDto> page = facturaRepository.findByEmpresaId(empresaId, pageable)
                .map(this::aResumen);
        return PaginaDto.desde(page);
    }

    @Transactional(readOnly = true)
    public byte[] exportarCsv(Long empresaId, LocalDate desde, LocalDate hasta) {
        List<FacturaJpa> facturas = facturaRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(CsvFacturas.fila("Tipo", "Numero", "Fecha", "Adquirente", "NIT", "Subtotal",
                "Impuestos", "Retenciones", "Total", "Estado", "CUFE")).append("\r\n");
        for (FacturaJpa f : facturas) {
            LocalDate fecha = f.getFechaEmision();
            if (desde != null && (fecha == null || fecha.isBefore(desde))) {
                continue;
            }
            if (hasta != null && (fecha == null || fecha.isAfter(hasta))) {
                continue;
            }
            csv.append(CsvFacturas.fila(
                    f.getTipoDocumento(),
                    f.getNumeroCompleto(),
                    fecha == null ? "" : fecha.toString(),
                    f.getAdquirenteNombre(),
                    f.getAdquirenteNumDoc(),
                    f.getSubtotal() == null ? "" : f.getSubtotal().toPlainString(),
                    f.getTotalImpuestos() == null ? "" : f.getTotalImpuestos().toPlainString(),
                    f.getTotalRetenciones() == null ? "" : f.getTotalRetenciones().toPlainString(),
                    f.getTotal() == null ? "" : f.getTotal().toPlainString(),
                    f.getEstado(),
                    f.getCufe() != null ? f.getCufe() : f.getCude())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private FacturaResumenDto aResumen(FacturaJpa f) {
        return new FacturaResumenDto(f.getId(), f.getTipoDocumento(), f.getNumeroCompleto(),
                f.getCufe() != null ? f.getCufe() : f.getCude(), f.getEstado(), f.getFechaEmision(),
                f.getAdquirenteNombre(), f.getAdquirenteNumDoc(), f.getTotal(), f.getTotalPagar(),
                f.getCreatedAt());
    }

    private static int acotarTamano(int tamano) {
        return Math.min(Math.max(tamano, 1), 100);
    }

    @Transactional(readOnly = true)
    public FacturaDetalleDto detalle(Long empresaId, Long id) {
        FacturaJpa f = facturaRepository.findById(id)
                .filter(x -> x.getEmpresa().getId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
        List<LineaFacturaRequest> lineas = f.getLineas().stream()
                .map(l -> new LineaFacturaRequest(l.getCodigoProducto(), l.getDescripcion(), l.getCantidad(),
                        l.getUnidadMedida(), l.getValorUnitario(), l.getValorDescuento(), l.getIvaPct(),
                        l.getIncPct(), l.getCodigoUnspsc()))
                .toList();
        AdquirenteRequest adq = new AdquirenteRequest(f.getAdquirenteTipoDoc(), f.getAdquirenteNumDoc(),
                f.getAdquirenteDv(), f.getAdquirenteNombre(), f.getAdquirenteEmail(), f.getAdquirenteDireccion(),
                f.getAdquirenteMunicipio(), f.getAdquirenteRegimen(), f.getAdquirenteResponsabilidad());
        List<FacturaEventoDto> eventos = eventoRepository.findByFacturaIdOrderByFechaDesc(f.getId()).stream()
                .map(this::aEvento)
                .toList();
        return new FacturaDetalleDto(f.getId(), f.getTipoDocumento(), f.getPrefijo(), f.getNumero(),
                f.getNumeroCompleto(), f.getCufe(), f.getCude(), f.getAmbiente(), f.getEstado(),
                f.getFechaEmision(), f.getHoraEmision(), f.getFechaVencimiento(), f.getMoneda(),
                f.getFormaPago(), f.getMedioPago(), adq, lineas, f.getSubtotal(), f.getTotalImpuestos(),
                f.getTotalRetenciones(), f.getTotal(), f.getTotalPagar(), f.getObservaciones(), f.getQrUrl(),
                f.getRespuestaDian(), f.getFechaValidacion(), f.getCreatedAt(), eventos);
    }

    @Transactional
    public FacturaEventoDto registrarEvento(Long empresaId, Long facturaId, String tipoEvento) {
        FacturaJpa f = facturaRepository.findById(facturaId)
                .filter(x -> x.getEmpresa().getId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
        EventoDian evento = EventoDian.porCodigo(tipoEvento)
                .orElseThrow(() -> new IllegalArgumentException("Evento DIAN no soportado: " + tipoEvento));
        EmpresaFeConfigJpa config = config(empresaId);
        return registrarEvento(f, evento, config, OffsetDateTime.now());
    }

    @Transactional
    public FacturaEventoDto registrarEvento(FacturaJpa f, EventoDian evento, EmpresaFeConfigJpa config,
                                            OffsetDateTime momento) {
        String cufe = f.getCufe() != null ? f.getCufe() : f.getCude();
        String idEvento = f.getNumeroCompleto() + "-" + evento.getCodigo();
        String cude = CufeCalculator.calcularEvento(f.getNumeroCompleto(), momento.toLocalDate(),
                momento.toLocalTime(), f.getEmpresa().getNit(), f.getAdquirenteNumDoc(), evento.getCodigo(),
                idEvento, f.getTipoDocumento(), config.getSoftwarePin());
        String xml = generadorEvento.generar(f, evento, cufe, cude, idEvento, momento,
                config.getSoftwareId(), config.getSoftwareSecurityCode(), null);
        String xmlFirmado = firma.firmar(xml, config);

        FacturaEventoJpa ev = new FacturaEventoJpa();
        ev.setFactura(f);
        ev.setTipoEvento(evento.getCodigo());
        ev.setEstado("REGISTRADO");
        ev.setCude(cude);
        ev.setXml(xmlFirmado);
        ev.setRespuesta(evento.getDescripcion());
        ev.setFecha(momento);
        return aEvento(eventoRepository.save(ev));
    }

    /**
     * Registra la aceptación tácita (034) si la factura no tiene reclamo (031), aceptación expresa (033)
     * ni aceptación tácita (034) previos. Devuelve vacío si ya existía un evento que la impide.
     */
    @Transactional
    public Optional<FacturaEventoDto> registrarAceptacionTacita(Long facturaId) {
        FacturaJpa f = facturaRepository.findById(facturaId).orElse(null);
        if (f == null) {
            return Optional.empty();
        }
        boolean bloqueado = eventoRepository.findByFacturaIdOrderByFechaDesc(facturaId).stream()
                .map(FacturaEventoJpa::getTipoEvento)
                .anyMatch(t -> "031".equals(t) || "033".equals(t) || "034".equals(t));
        if (bloqueado) {
            return Optional.empty();
        }
        EmpresaFeConfigJpa config = config(f.getEmpresa().getId());
        return Optional.of(registrarEvento(f, EventoDian.ACEPTACION_TACITA, config, OffsetDateTime.now()));
    }

    private FacturaEventoDto aEvento(FacturaEventoJpa e) {
        String descripcion = EventoDian.porCodigo(e.getTipoEvento())
                .map(EventoDian::getDescripcion)
                .orElse(e.getTipoEvento());
        return new FacturaEventoDto(e.getId(), e.getTipoEvento(), descripcion, e.getEstado(), e.getFecha());
    }

    @Transactional(readOnly = true)
    public String xmlFirmado(Long empresaId, Long id) {
        return facturaRepository.findById(id)
                .filter(x -> x.getEmpresa().getId().equals(empresaId))
                .map(FacturaJpa::getXmlFirmado)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Long empresaId, Long id) {
        FacturaJpa f = facturaRepository.findById(id)
                .filter(x -> x.getEmpresa().getId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
        EmpresaJpa empresa = f.getEmpresa();
        List<LineaFactura> lineas = new ArrayList<>();
        for (FacturaLineaJpa l : f.getLineas()) {
            lineas.add(new LineaFactura(l.getNumeroLinea(), l.getCodigoProducto(), l.getDescripcion(),
                    l.getCantidad(), l.getUnidadMedida(), l.getValorUnitario(), l.getValorDescuento(),
                    l.getIvaPct(), l.getIvaValor(), l.getIncPct(), l.getIncValor(), l.getTotalLinea(),
                    l.getCodigoUnspsc()));
        }
        List<ImpuestoFactura> impuestos = f.getImpuestos().stream()
                .map(i -> new ImpuestoFactura(i.getTipo(), i.getNombre(), i.getBase(), i.getPorcentaje(), i.getValor()))
                .toList();
        DatosEmisor emisor = new DatosEmisor(mapearTipoDocumento(empresa.getTipoDocumento()), empresa.getNit(),
                NitDigitoVerificacionRegla.calcularDv(empresa.getNit()), empresa.getRazonSocial(), empresa.getRegimen(),
                "R-99-PN", empresa.getDireccion(), empresa.getCiudad(), empresa.getDepartamento(), empresa.getEmail(),
                empresa.isGranContribuyente());
        DatosAdquirente adq = new DatosAdquirente(f.getAdquirenteTipoDoc(), f.getAdquirenteNumDoc(),
                f.getAdquirenteDv(), f.getAdquirenteNombre(), f.getAdquirenteEmail(), f.getAdquirenteDireccion(),
                f.getAdquirenteMunicipio(), f.getAdquirenteRegimen(), f.getAdquirenteResponsabilidad());
        FacturaDatos datos = new FacturaDatos(TipoDocumento.porCodigo(f.getTipoDocumento()), f.getNumeroCompleto(),
                f.getPrefijo(), f.getNumero(), f.getFechaEmision(), f.getHoraEmision(), f.getFechaVencimiento(),
                f.getAmbiente(), f.getMoneda(), f.getFormaPago(), f.getMedioPago(), f.getTipoOperacion(), emisor, adq,
                lineas, impuestos, f.getSubtotal(), f.getTotalImpuestos(), f.getTotalRetenciones(), f.getTotal(),
                f.getTotalPagar(), f.getObservaciones(), f.getCufeReferencia(), f.getNumeroReferencia(), null, null,
                null, null, null, null, null, null, 0, 0);
        String cufe = f.getCufe() != null ? f.getCufe() : f.getCude();
        BrandingDto marca = null;
        if (branding != null) {
            try {
                marca = branding.obtener(empresaId);
            } catch (RuntimeException ex) {
                log.warn("No se pudo aplicar la marca de la empresa {} al PDF: {}", empresaId, ex.getMessage());
            }
        }
        if (marca == null) {
            return generadorPdf.generar(datos, cufe, f.getQrUrl());
        }
        return generadorPdf.generar(datos, cufe, f.getQrUrl(), marca);
    }

    private FacturaJpa persistir(EmpresaJpa empresa, Long usuarioId, EmpresaFeConfigJpa config, FacturaDatos d,
                                  String cufe, String qrUrl, String xml, String xmlFirmado,
                                  ResultadoTransmision resultado, String tipoDocumento) {
        FacturaJpa f = new FacturaJpa();
        f.setEmpresa(empresa);
        f.setTipoDocumento(tipoDocumento == null ? d.tipo().getCodigo() : tipoDocumento);
        f.setPrefijo(d.prefijo());
        f.setNumero(d.numero());
        f.setNumeroCompleto(d.numeroCompleto());
        if (d.tipo().esNota()) {
            f.setCude(cufe);
        } else {
            f.setCufe(cufe);
        }
        f.setAmbiente((short) d.ambiente());
        f.setFechaEmision(d.fechaEmision());
        f.setHoraEmision(d.horaEmision());
        f.setFechaVencimiento(d.fechaVencimiento());
        f.setMoneda(d.moneda());
        f.setFormaPago(d.formaPago());
        f.setMedioPago(d.medioPago());
        f.setTipoOperacion(d.tipoOperacion());
        f.setAdquirenteTipoDoc(d.adquirente().tipoDocumento());
        f.setAdquirenteNumDoc(d.adquirente().numeroDocumento());
        f.setAdquirenteDv(d.adquirente().digitoVerificacion());
        f.setAdquirenteNombre(d.adquirente().nombre());
        f.setAdquirenteEmail(d.adquirente().email());
        f.setAdquirenteDireccion(d.adquirente().direccion());
        f.setAdquirenteMunicipio(d.adquirente().municipio());
        f.setAdquirenteRegimen(d.adquirente().regimen());
        f.setAdquirenteResponsabilidad(d.adquirente().responsabilidadFiscal());
        f.setSubtotal(d.valorSubtotal());
        f.setTotalImpuestos(d.totalImpuestos());
        f.setTotalRetenciones(d.totalRetenciones());
        f.setTotal(d.total());
        f.setTotalPagar(d.totalPagar());
        f.setObservaciones(d.observaciones());
        f.setEstado(resultado.exitoso() ? "ACEPTADO" : "RECHAZADA");
        f.setXmlGenerado(xml);
        f.setXmlFirmado(xmlFirmado);
        f.setQrUrl(qrUrl);
        f.setRespuestaDian(resultado.respuestaCruda());
        f.setCufeReferencia(d.cufeReferencia());
        f.setNumeroReferencia(d.numeroReferencia());
        f.setUsuarioCreador(usuarioId);
        if (resultado.exitoso()) {
            f.setFechaValidacion(java.time.OffsetDateTime.now());
        }
        int i = 1;
        for (LineaFactura l : d.lineas()) {
            FacturaLineaJpa jl = new FacturaLineaJpa();
            jl.setNumeroLinea(i++);
            jl.setCodigoProducto(l.codigoProducto());
            jl.setDescripcion(l.descripcion());
            jl.setCantidad(l.cantidad());
            jl.setUnidadMedida(l.unidadMedida());
            jl.setValorUnitario(l.valorUnitario());
            jl.setValorDescuento(l.valorDescuento());
            jl.setIvaPct(l.ivaPct());
            jl.setIvaValor(l.ivaValor());
            jl.setIncPct(l.incPct());
            jl.setIncValor(l.incValor());
            jl.setTotalLinea(l.totalLinea());
            jl.setCodigoUnspsc(l.codigoUnspsc());
            f.addLinea(jl);
        }
        if (d.impuestos() != null) {
            for (ImpuestoFactura imp : d.impuestos()) {
                FacturaImpuestoJpa ji = new FacturaImpuestoJpa();
                ji.setTipo(imp.tipo());
                ji.setNombre(imp.nombre());
                ji.setBase(imp.base());
                ji.setPorcentaje(imp.porcentaje());
                ji.setValor(imp.valor());
                f.addImpuesto(ji);
            }
        }
        return facturaRepository.save(f);
    }

    private FacturaDatos construirDatos(EmpresaJpa empresa, EmpresaFeConfigJpa config, FacturaRequest req,
                                         long numero, String numeroCompleto) {
        return construirDatos(empresa, config, SolicitudEmisionDianDto.desde(req), numero, numeroCompleto);
    }

    private FacturaDatos construirDatos(EmpresaJpa empresa, EmpresaFeConfigJpa config, SolicitudEmisionDianDto req,
                                         long numero, String numeroCompleto) {
        TipoDocumento tipo = TipoDocumento.porCodigo(req.tipoDocumento());
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now().withNano(0);
        String softwareId = config.getSoftwareId();
        String softwarePin = config.getSoftwarePin();
        String securityCode = CufeCalculator.softwareSecurityCode(softwareId, softwarePin, numeroCompleto);

        List<LineaFactura> lineas = new ArrayList<>();
        List<ImpuestoFactura> impuestos = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        BigDecimal totalInc = BigDecimal.ZERO;
        BigDecimal baseIva = BigDecimal.ZERO;
        BigDecimal baseInc = BigDecimal.ZERO;
        int i = 1;
        if (req.lineas() != null) {
            for (LineaFacturaRequest lr : req.lineas()) {
                BigDecimal cant = nz(lr.cantidad(), BigDecimal.ONE);
                BigDecimal vu = nz(lr.valorUnitario(), BigDecimal.ZERO);
                BigDecimal desc = nz(lr.descuento(), BigDecimal.ZERO);
                BigDecimal ivaPct = nz(lr.ivaPct(), BigDecimal.ZERO);
                BigDecimal incPct = nz(lr.incPct(), BigDecimal.ZERO);
                BigDecimal bruto = cant.multiply(vu).setScale(2, RoundingMode.HALF_UP);
                BigDecimal base = bruto.subtract(desc).setScale(2, RoundingMode.HALF_UP);
                BigDecimal ivaVal = base.multiply(ivaPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal incVal = base.multiply(incPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal total = base.add(ivaVal).add(incVal).setScale(2, RoundingMode.HALF_UP);
                String codProducto = (lr.codigoProducto() == null || lr.codigoProducto().isBlank())
                        ? lr.codigoUnspsc() : lr.codigoProducto();
                lineas.add(new LineaFactura(i++, codProducto, lr.descripcion(), cant,
                        lr.unidadMedida() == null ? "94" : lr.unidadMedida(), vu, desc, ivaPct, ivaVal, incPct,
                        incVal, total, lr.codigoUnspsc()));
                subtotal = subtotal.add(base);
                baseIva = baseIva.add(base);
                baseInc = baseInc.add(base);
                totalIva = totalIva.add(ivaVal);
                totalInc = totalInc.add(incVal);
            }
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        totalIva = totalIva.setScale(2, RoundingMode.HALF_UP);
        totalInc = totalInc.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalImpuestos = totalIva.add(totalInc).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(totalImpuestos).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRetenciones = BigDecimal.ZERO;
        BigDecimal totalPagar = total.subtract(totalRetenciones).setScale(2, RoundingMode.HALF_UP);

        if (totalIva.compareTo(BigDecimal.ZERO) > 0) {
            impuestos.add(new ImpuestoFactura(COD_IVA, "IVA", baseIva, BigDecimal.valueOf(19), totalIva));
        }
        if (totalInc.compareTo(BigDecimal.ZERO) > 0) {
            impuestos.add(new ImpuestoFactura(COD_INC, "INC", baseInc, BigDecimal.ZERO, totalInc));
        }
        if (req.retenciones() != null) {
            for (RetencionDianDto retencion : req.retenciones()) {
                BigDecimal base = nz(retencion.base(), subtotal);
                BigDecimal valor = nz(retencion.valor(), BigDecimal.ZERO);
                if (valor.signum() == 0 && retencion.porcentaje() != null) {
                    valor = base.multiply(retencion.porcentaje())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
                impuestos.add(new ImpuestoFactura(codigoRetencion(retencion.codigo()),
                        retencion.nombre() == null ? "Retención" : retencion.nombre(), base,
                        nz(retencion.porcentaje(), BigDecimal.ZERO), valor));
                totalRetenciones = totalRetenciones.add(valor);
            }
        }
        totalRetenciones = totalRetenciones.setScale(2, RoundingMode.HALF_UP);
        totalPagar = total.subtract(totalRetenciones).setScale(2, RoundingMode.HALF_UP);

        DatosEmisor emisor = new DatosEmisor(mapearTipoDocumento(empresa.getTipoDocumento()), empresa.getNit(),
                NitDigitoVerificacionRegla.calcularDv(empresa.getNit()), empresa.getRazonSocial(), empresa.getRegimen(),
                config.getResponsabilidadFiscal(), empresa.getDireccion(), empresa.getCiudad(),
                empresa.getDepartamento(), empresa.getEmail(), empresa.isGranContribuyente());

        AdquirenteRequest ar = req.adquirente();
        DatosAdquirente adquirente = new DatosAdquirente(
                ar == null ? "13" : nvl(ar.tipoDocumento()).isEmpty() ? "13" : ar.tipoDocumento(),
                ar == null ? null : ar.numeroDocumento(),
                ar == null ? null : ar.digitoVerificacion(),
                ar == null ? null : ar.nombre(),
                ar == null ? null : ar.email(),
                ar == null ? null : ar.direccion(),
                ar == null ? null : ar.municipio(),
                ar == null || ar.regimen() == null ? "ORDINARIO" : ar.regimen(),
                ar == null || ar.responsabilidadFiscal() == null ? "R-99-PN" : ar.responsabilidadFiscal());

        String observaciones = req.observaciones();
        if (req.motivo() != null && !req.motivo().isBlank()) {
            observaciones = nvl(observaciones).isEmpty() ? "Motivo: " + req.motivo()
                    : nvl(observaciones) + " | Motivo: " + req.motivo();
        }
        return new FacturaDatos(tipo, numeroCompleto, nvl(config.getPrefijo()), numero, hoy, ahora,
                req.fechaVencimiento(), config.getAmbiente(), "COP",
                req.formaPago() == null ? "1" : req.formaPago(),
                req.medioPago() == null ? "10" : req.medioPago(),
                req.tipoOperacion() == null ? "10" : req.tipoOperacion(),
                emisor, adquirente, lineas, impuestos, subtotal, totalImpuestos, totalRetenciones, total, totalPagar,
                observaciones, req.cufeReferencia(), req.numeroReferencia(), softwareId, softwarePin,
                securityCode, config.getTestSetId(), config.getResolucionNumero(), config.getResolucionFecha(),
                config.getRangoVigenciaDesde(), config.getRangoVigenciaHasta(),
                config.getRangoDesde() == null ? 0 : config.getRangoDesde(),
                config.getRangoHasta() == null ? 0 : config.getRangoHasta());
    }

    private String mapearTipoDocumento(String tipo) {
        if (tipo == null) {
            return "31";
        }
        return switch (tipo.toUpperCase()) {
            case "NIT" -> "31";
            case "CC", "CEDULA", "CÉDULA" -> "13";
            case "CE" -> "22";
            case "PA", "PASAPORTE" -> "41";
            case "TI" -> "12";
            default -> "31";
        };
    }

    private EmpresaJpa empresa(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }

    private EmpresaFeConfigJpa config(Long empresaId) {
        return configRepository.findConEmpresa(empresaId).orElseGet(() -> {
            EmpresaJpa e = empresa(empresaId);
            EmpresaFeConfigJpa c = new EmpresaFeConfigJpa();
            c.setEmpresa(e);
            c.setCanal("SIMULADO");
            return configRepository.save(c);
        });
    }

    private ErrorValidacionDto aError(ResultadoRegla r) {
        return new ErrorValidacionDto(r.codigoRegla(), r.severidad() == null ? null : r.severidad().name(),
                r.mensaje(), r.linea(), r.campo(), r.valorEsperado(), r.valorEncontrado());
    }

    private void guardarPdf(FacturaJpa factura, FacturaDatos datos, String cufe, String qrUrl) {
        try {
            byte[] pdf = generadorPdf.generar(datos, cufe, qrUrl);
            String base = System.getProperty("magnetixdian.upload.dir", "./uploads");
            Path directorio = Paths.get(base, "facturas", String.valueOf(factura.getEmpresa().getId()));
            Files.createDirectories(directorio);
            String nombre = nvl(factura.getNumeroCompleto()).replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";
            Path archivo = directorio.resolve(nombre);
            Files.write(archivo, pdf);
            factura.setPdfPath(archivo.toAbsolutePath().toString());
            facturaRepository.save(factura);
        } catch (Exception ex) {
            log.warn("No se pudo persistir el PDF de la factura {}: {}", factura.getNumeroCompleto(), ex.getMessage());
        }
    }

    private String extraerTrackId(String respuesta, String cufeRespaldo) {
        if (respuesta != null && !respuesta.isBlank()) {
            try {
                JsonNode raiz = MAPPER.readTree(respuesta);
                for (String clave : List.of("trackId", "track_id", "TrackId", "zipKey", "ZipKey", "ZIPKey")) {
                    JsonNode valor = raiz.findValue(clave);
                    if (valor != null && !valor.isNull() && !valor.asText().isBlank()) {
                        return valor.asText();
                    }
                }
            } catch (IOException ignored) {
            }
            Matcher matcher = PATRON_TRACK.matcher(respuesta);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return cufeRespaldo;
    }

    private String motivoTransmision(ResultadoTransmision resultado) {
        if (resultado == null) {
            return "La transmisión no devolvió resultado.";
        }
        if (resultado.mensaje() != null && !resultado.mensaje().isBlank()) {
            return resultado.mensaje();
        }
        if (resultado.respuestaCruda() != null && !resultado.respuestaCruda().isBlank()) {
            return resultado.respuestaCruda();
        }
        return "La DIAN rechazó el documento.";
    }

    private void publicarEvento(FacturaJpa factura, FacturaDatos datos, String trackId, String motivo) {
        if (eventPublisher == null) {
            return;
        }
        eventPublisher.publishEvent(new FacturaEmitidaEvent(
                factura.getEmpresa().getId(), factura.getId(), factura.getNumeroCompleto(),
                factura.getTipoDocumento(), factura.getCufe(), factura.getCude(), trackId, factura.getQrUrl(),
                factura.getEstado(), factura.getFechaEmision(), datos.nitAdquirente(),
                terceroId(factura.getEmpresa().getId(), factura.getAdquirenteTipoDoc(),
                        factura.getAdquirenteNumDoc()),
                factura.getSubtotal(), factura.getTotalImpuestos(), factura.getTotalRetenciones(),
                factura.getTotal(), factura.getTotalPagar(), motivo));
    }

    private boolean estaRechazada(String estado) {
        return estado != null && (estado.equalsIgnoreCase("RECHAZADO") || estado.equalsIgnoreCase("RECHAZADA"));
    }

    private boolean esRetencion(String codigo) {
        return "05".equals(codigo) || "06".equals(codigo) || "07".equals(codigo);
    }

    private String codigoRetencion(String codigo) {
        if (codigo != null && !codigo.isBlank()) {
            String normalizado = codigo.trim();
            if (esRetencion(normalizado)) {
                return normalizado;
            }
            if (normalizado.toUpperCase().contains("RETE")) {
                return "05";
            }
        }
        return "05";
    }

    private Long terceroId(Long empresaId, String tipoDocumento, String numeroDocumento) {
        if (terceroRepository == null || numeroDocumento == null || numeroDocumento.isBlank()) {
            return null;
        }
        return terceroRepository.findByEmpresaIdAndTipoDocumentoAndNumeroDocumento(
                        empresaId, tipoDocumento, numeroDocumento)
                .map(tercero -> tercero.getId())
                .orElse(null);
    }

    private BigDecimal nz(BigDecimal v) {
        return nz(v, BigDecimal.ZERO);
    }

    private BigDecimal nz(BigDecimal v, BigDecimal def) {
        return v == null ? def : v;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
