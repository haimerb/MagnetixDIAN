package com.magnetixdian.application;

import com.magnetixdian.domain.cufe.CufeCalculator;
import com.magnetixdian.domain.cufe.QrUrlGenerator;
import com.magnetixdian.domain.factura.DatosAdquirente;
import com.magnetixdian.domain.factura.DatosEmisor;
import com.magnetixdian.domain.factura.FacturaDatos;
import com.magnetixdian.domain.factura.ImpuestoFactura;
import com.magnetixdian.domain.factura.LineaFactura;
import com.magnetixdian.domain.factura.TipoDocumento;
import com.magnetixdian.domain.model.ResultadoRegla;
import com.magnetixdian.domain.model.Severidad;
import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;
import com.magnetixdian.domain.rule.factura.ContextoFactura;
import com.magnetixdian.domain.rule.factura.ValidadorFactura;
import com.magnetixdian.domain.transmision.ResultadoTransmision;
import com.magnetixdian.infrastructure.pdf.GeneradorPdfFactura;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.FacturaEventoJpa;
import com.magnetixdian.infrastructure.persistence.FacturaEventoRepository;
import com.magnetixdian.infrastructure.persistence.FacturaImpuestoJpa;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaLineaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaRepository;
import com.magnetixdian.infrastructure.xml.GeneradorAttachedDocument;
import com.magnetixdian.infrastructure.xml.GeneradorUblFactura;
import com.magnetixdian.interfaces.dto.AdquirenteRequest;
import com.magnetixdian.interfaces.dto.ErrorValidacionDto;
import com.magnetixdian.interfaces.dto.FacturaDetalleDto;
import com.magnetixdian.interfaces.dto.FacturaRequest;
import com.magnetixdian.interfaces.dto.FacturaResumenDto;
import com.magnetixdian.interfaces.dto.LineaFacturaRequest;
import com.magnetixdian.interfaces.dto.ResultadoEmisionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio principal de facturación electrónica: valida, numera, genera el UBL,
 * firma, calcula el CUFE/CUDE, produce el PDF/QR, transmite y persiste el
 * documento.
 */
@Service
public class ServicioFacturacion {

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
                               ServicioTransmision transmision) {
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

    @Transactional
    public ResultadoEmisionDto emitir(Long empresaId, Long usuarioId, FacturaRequest req) {
        EmpresaJpa empresa = empresa(empresaId);
        EmpresaFeConfigJpa config = config(empresaId);
        TipoDocumento tipo = TipoDocumento.porCodigo(req.tipoDocumento());

        FacturaDatos preliminar = construirDatos(empresa, config, req, 0L, "");
        List<ResultadoRegla> resultados = validador.validar(new ContextoFactura(preliminar, config));
        if (validador.tieneErrores(resultados)) {
            return new ResultadoEmisionDto(false, null, null, null, "RECHAZADO", null,
                    "El documento tiene errores de validación", resultados.stream().filter(x -> !x.valida())
                    .map(this::aError).toList());
        }

        long numero = numeracion.siguiente(empresaId, nvl(config.getPrefijo()), tipo.getCodigo());
        String numeroCompleto = nvl(config.getPrefijo()) + numero;
        FacturaDatos datos = construirDatos(empresa, config, req, numero, numeroCompleto);

        String cufe = CufeCalculator.calcular(datos);
        String qrUrl = QrUrlGenerator.generar(cufe, datos.ambiente());
        String xml = generadorUbl.generar(datos, cufe);
        String xmlFirmado = firma.firmar(xml, config);

        ResultadoTransmision transmisionResultado = transmision.enviar(xmlFirmado, cufe, datos, config);

        FacturaJpa factura = persistir(empresa, usuarioId, config, datos, cufe, qrUrl, xml, xmlFirmado,
                transmisionResultado);

        FacturaEventoJpa evento = new FacturaEventoJpa();
        evento.setFactura(factura);
        evento.setTipoEvento("EMISION");
        evento.setEstado(transmisionResultado.exitoso() ? "ACEPTADO" : "RECHAZADO");
        evento.setXml(xmlFirmado);
        evento.setRespuesta(transmisionResultado.respuestaCruda());
        eventoRepository.save(evento);

        return new ResultadoEmisionDto(true, factura.getId(), numeroCompleto, cufe, factura.getEstado(),
                qrUrl, transmisionResultado.mensaje(), List.of());
    }

    @Transactional(readOnly = true)
    public List<FacturaResumenDto> listar(Long empresaId) {
        return facturaRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId).stream()
                .map(f -> new FacturaResumenDto(f.getId(), f.getTipoDocumento(), f.getNumeroCompleto(),
                        f.getCufe() != null ? f.getCufe() : f.getCude(), f.getEstado(), f.getFechaEmision(),
                        f.getAdquirenteNombre(), f.getAdquirenteNumDoc(), f.getTotal(), f.getTotalPagar(),
                        f.getCreatedAt()))
                .toList();
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
        return new FacturaDetalleDto(f.getId(), f.getTipoDocumento(), f.getPrefijo(), f.getNumero(),
                f.getNumeroCompleto(), f.getCufe(), f.getCude(), f.getAmbiente(), f.getEstado(),
                f.getFechaEmision(), f.getHoraEmision(), f.getFechaVencimiento(), f.getMoneda(),
                f.getFormaPago(), f.getMedioPago(), adq, lineas, f.getSubtotal(), f.getTotalImpuestos(),
                f.getTotalRetenciones(), f.getTotal(), f.getTotalPagar(), f.getObservaciones(), f.getQrUrl(),
                f.getRespuestaDian(), f.getFechaValidacion(), f.getCreatedAt());
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
        return generadorPdf.generar(datos, cufe, f.getQrUrl());
    }

    private FacturaJpa persistir(EmpresaJpa empresa, Long usuarioId, EmpresaFeConfigJpa config, FacturaDatos d,
                                 String cufe, String qrUrl, String xml, String xmlFirmado,
                                 ResultadoTransmision resultado) {
        FacturaJpa f = new FacturaJpa();
        f.setEmpresa(empresa);
        f.setTipoDocumento(d.tipo().getCodigo());
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
        f.setEstado(resultado.exitoso() ? "ACEPTADO" : "RECHAZADO");
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

        return new FacturaDatos(tipo, numeroCompleto, nvl(config.getPrefijo()), numero, hoy, ahora,
                req.fechaVencimiento(), config.getAmbiente(), "COP",
                req.formaPago() == null ? "1" : req.formaPago(),
                req.medioPago() == null ? "10" : req.medioPago(),
                req.tipoOperacion() == null ? "10" : req.tipoOperacion(),
                emisor, adquirente, lineas, impuestos, subtotal, totalImpuestos, totalRetenciones, total, totalPagar,
                req.observaciones(), req.cufeReferencia(), req.numeroReferencia(), softwareId, softwarePin,
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

    private BigDecimal nz(BigDecimal v, BigDecimal def) {
        return v == null ? def : v;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
