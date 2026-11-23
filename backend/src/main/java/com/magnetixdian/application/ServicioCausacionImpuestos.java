package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.ImpuestoCausacionJpa;
import com.magnetixdian.infrastructure.persistence.ImpuestoCausacionRepository;
import com.magnetixdian.infrastructure.persistence.ImpuestoPagoJpa;
import com.magnetixdian.infrastructure.persistence.ImpuestoPagoRepository;
import com.magnetixdian.interfaces.dto.CausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.PagoImpuestoDto;
import com.magnetixdian.interfaces.dto.ResultadoCalculoImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudCausacionImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudPagoImpuestoDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ServicioCausacionImpuestos {

    private final ImpuestoCausacionRepository causacionRepository;
    private final ImpuestoPagoRepository pagoRepository;
    private final ServicioTarifasImpuesto servicioTarifas;
    private final ServicioResponsabilidadesFiscales servicioResponsabilidades;

    public ServicioCausacionImpuestos(ImpuestoCausacionRepository causacionRepository,
                                      ImpuestoPagoRepository pagoRepository,
                                      ServicioTarifasImpuesto servicioTarifas,
                                      ServicioResponsabilidadesFiscales servicioResponsabilidades) {
        this.causacionRepository = causacionRepository;
        this.pagoRepository = pagoRepository;
        this.servicioTarifas = servicioTarifas;
        this.servicioResponsabilidades = servicioResponsabilidades;
    }

    public CausacionImpuestoDto causar(Long empresaId, SolicitudCausacionImpuestoDto solicitud) {
        String impuesto = requerir(solicitud.impuestoCodigo(), "El código de impuesto es obligatorio.");
        String origen = requerir(solicitud.origen(), "El origen de la causación es obligatorio.");
        if (solicitud.origenId() == null) {
            throw new IllegalArgumentException("El identificador de origen es obligatorio.");
        }
        if (solicitud.baseImponible() == null || solicitud.baseImponible().signum() < 0) {
            throw new IllegalArgumentException("La base imponible debe ser mayor o igual a cero.");
        }
        LocalDate fecha = solicitud.fecha() == null ? LocalDate.now() : solicitud.fecha();

        var existente = causacionRepository.findByEmpresaIdAndOrigenAndOrigenIdAndImpuestoCodigo(
                empresaId, origen, solicitud.origenId(), impuesto);
        if (existente.isPresent()) {
            return mapear(existente.get());
        }

        if (!servicioResponsabilidades.esResponsable(
                empresaId, impuesto, fecha, solicitud.terceroId())) {
            throw new IllegalStateException(
                    "No hay responsabilidad fiscal vigente para " + impuesto + " en la fecha indicada.");
        }

        ResultadoCalculoImpuestoDto calculo = servicioTarifas.calcular(
                impuesto, fecha.getYear(), solicitud.conceptoCodigo(), solicitud.baseImponible());

        ImpuestoCausacionJpa causacion = new ImpuestoCausacionJpa();
        causacion.setEmpresaId(empresaId);
        causacion.setImpuestoCodigo(impuesto);
        causacion.setOrigen(origen);
        causacion.setOrigenId(solicitud.origenId());
        causacion.setTerceroId(solicitud.terceroId());
        causacion.setPeriodo(String.format(Locale.ROOT, "%04d-%02d", fecha.getYear(), fecha.getMonthValue()));
        causacion.setFecha(fecha);
        causacion.setConceptoCodigo(calculo.conceptoCodigo());
        causacion.setBaseImponible(solicitud.baseImponible());
        causacion.setTarifa(calculo.tarifa());
        causacion.setValor(calculo.valor());
        causacion.setEstado("CAUSADO");
        causacion.setObservaciones(recortar(solicitud.observaciones()));
        return mapear(causacionRepository.save(causacion));
    }

    public List<CausacionImpuestoDto> listarPorPeriodo(Long empresaId, String periodo) {
        return causacionRepository.findByEmpresaIdAndPeriodoOrderByFechaAscIdAsc(empresaId, periodo)
                .stream()
                .map(this::mapear)
                .toList();
    }

    public List<CausacionImpuestoDto> listarPorTercero(Long empresaId, Long terceroId) {
        return causacionRepository.findByEmpresaIdAndTerceroIdOrderByFechaDescIdDesc(empresaId, terceroId)
                .stream()
                .map(this::mapear)
                .toList();
    }

    public PagoImpuestoDto registrarPago(Long empresaId,
                                         Long causacionId,
                                         SolicitudPagoImpuestoDto solicitud) {
        if (solicitud == null || solicitud.valor() == null || solicitud.valor().signum() <= 0) {
            throw new IllegalArgumentException("El valor del pago debe ser mayor a cero.");
        }
        ImpuestoCausacionJpa causacion = causacionRepository.findById(causacionId)
                .filter(registro -> registro.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Causación no encontrada."));
        if ("ANULADO".equals(causacion.getEstado())) {
            throw new IllegalStateException("No se pueden registrar pagos sobre una causación anulada.");
        }
        BigDecimal totalPagado = pagoRepository.sumarPorCausacion(causacionId)
                .add(solicitud.valor());
        if (totalPagado.compareTo(causacion.getValor()) > 0) {
            throw new IllegalArgumentException("El pago supera el valor causado.");
        }

        ImpuestoPagoJpa pago = new ImpuestoPagoJpa();
        pago.setEmpresaId(empresaId);
        pago.setCausacionId(causacionId);
        pago.setFecha(solicitud.fecha() == null ? LocalDate.now() : solicitud.fecha());
        pago.setValor(solicitud.valor());
        pago.setComprobante(solicitud.comprobante());
        ImpuestoPagoJpa guardado = pagoRepository.save(pago);

        if (totalPagado.compareTo(causacion.getValor()) >= 0) {
            causacion.setEstado("PAGADO");
            causacionRepository.save(causacion);
        }
        return mapear(guardado);
    }

    public CausacionImpuestoDto anular(Long empresaId, Long causacionId) {
        ImpuestoCausacionJpa causacion = causacionRepository.findById(causacionId)
                .filter(registro -> registro.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Causación no encontrada."));
        causacion.setEstado("ANULADO");
        return mapear(causacionRepository.save(causacion));
    }

    private CausacionImpuestoDto mapear(ImpuestoCausacionJpa causacion) {
        BigDecimal pagado = pagoRepository.sumarPorCausacion(causacion.getId());
        return new CausacionImpuestoDto(
                causacion.getId(),
                causacion.getEmpresaId(),
                causacion.getImpuestoCodigo(),
                causacion.getOrigen(),
                causacion.getOrigenId(),
                causacion.getTerceroId(),
                causacion.getPeriodo(),
                causacion.getFecha(),
                causacion.getConceptoCodigo(),
                causacion.getBaseImponible(),
                causacion.getTarifa(),
                causacion.getValor(),
                causacion.getEstado(),
                pagado,
                causacion.getObservaciones());
    }

    private PagoImpuestoDto mapear(ImpuestoPagoJpa pago) {
        return new PagoImpuestoDto(
                pago.getId(),
                pago.getCausacionId(),
                pago.getFecha(),
                pago.getValor(),
                pago.getComprobante());
    }

    private String requerir(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String recortar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= 300 ? valor : valor.substring(0, 300);
    }
}
