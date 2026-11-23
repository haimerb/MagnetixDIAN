package com.magnetixdian.application;

import com.magnetixdian.infrastructure.persistence.CalendarioTributarioJpa;
import com.magnetixdian.infrastructure.persistence.CalendarioTributarioRepository;
import com.magnetixdian.infrastructure.persistence.DeclaracionImpuestoJpa;
import com.magnetixdian.infrastructure.persistence.DeclaracionImpuestoRepository;
import com.magnetixdian.infrastructure.persistence.ImpuestoCausacionRepository;
import com.magnetixdian.infrastructure.persistence.ImpuestoPagoRepository;
import com.magnetixdian.interfaces.dto.CalendarioTributarioDto;
import com.magnetixdian.interfaces.dto.DeclaracionImpuestoDto;
import com.magnetixdian.interfaces.dto.SolicitudDeclaracionImpuestoDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ServicioDeclaracionesImpuesto {

    private final DeclaracionImpuestoRepository declaracionRepository;
    private final ImpuestoCausacionRepository causacionRepository;
    private final ImpuestoPagoRepository pagoRepository;
    private final CalendarioTributarioRepository calendarioRepository;

    public ServicioDeclaracionesImpuesto(DeclaracionImpuestoRepository declaracionRepository,
                                         ImpuestoCausacionRepository causacionRepository,
                                         ImpuestoPagoRepository pagoRepository,
                                         CalendarioTributarioRepository calendarioRepository) {
        this.declaracionRepository = declaracionRepository;
        this.causacionRepository = causacionRepository;
        this.pagoRepository = pagoRepository;
        this.calendarioRepository = calendarioRepository;
    }

    public DeclaracionImpuestoDto generarBorrador(Long empresaId, SolicitudDeclaracionImpuestoDto solicitud) {
        String impuesto = requerir(solicitud.impuestoCodigo(), "El código de impuesto es obligatorio.");
        if (solicitud.fechaDesde() == null || solicitud.fechaHasta() == null) {
            throw new IllegalArgumentException("El rango de fechas de la declaración es obligatorio.");
        }
        if (solicitud.fechaHasta().isBefore(solicitud.fechaDesde())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
        }

        Integer anio = solicitud.anioGravable() == null
                ? solicitud.fechaDesde().getYear()
                : solicitud.anioGravable();
        String periodo = solicitud.periodo() == null || solicitud.periodo().isBlank()
                ? String.format(Locale.ROOT, "%04d-%02d",
                        solicitud.fechaDesde().getYear(), solicitud.fechaDesde().getMonthValue())
                : solicitud.periodo().trim();

        BigDecimal totalCausado = causacionRepository
                .findByEmpresaIdAndImpuestoCodigoAndFechaBetweenOrderByFechaAscIdAsc(
                        empresaId, impuesto, solicitud.fechaDesde(), solicitud.fechaHasta())
                .stream()
                .filter(causacion -> !"ANULADO".equals(causacion.getEstado()))
                .map(causacion -> causacion.getValor() == null ? BigDecimal.ZERO : causacion.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPagado = pagoRepository.sumarPorEmpresaImpuestoPeriodo(
                empresaId, impuesto, solicitud.fechaDesde(), solicitud.fechaHasta());
        BigDecimal saldo = totalCausado.subtract(totalPagado).max(BigDecimal.ZERO);
        LocalDate fechaLimite = calendarioRepository
                .findFirstByImpuestoCodigoAndAnioGravableAndPeriodo(impuesto, anio, periodo)
                .map(CalendarioTributarioJpa::getFechaLimite)
                .orElseGet(() -> solicitud.fechaHasta().plusMonths(1).withDayOfMonth(20));

        var existente = declaracionRepository.findByEmpresaIdAndImpuestoCodigoAndAnioGravableAndPeriodo(
                empresaId, impuesto, anio, periodo);
        if (existente.isPresent() && "PRESENTADA".equals(existente.get().getEstado())) {
            throw new IllegalStateException("La declaración ya fue presentada y no puede recalcularse.");
        }
        DeclaracionImpuestoJpa declaracion = existente.orElseGet(DeclaracionImpuestoJpa::new);
        declaracion.setEmpresaId(empresaId);
        declaracion.setImpuestoCodigo(impuesto);
        declaracion.setAnioGravable(anio);
        declaracion.setPeriodo(periodo);
        declaracion.setFechaDesde(solicitud.fechaDesde());
        declaracion.setFechaHasta(solicitud.fechaHasta());
        declaracion.setFechaLimite(fechaLimite);
        declaracion.setEstado("BORRADOR");
        declaracion.setTotalCausado(totalCausado);
        declaracion.setTotalPagado(totalPagado);
        declaracion.setSaldo(saldo);
        return mapear(declaracionRepository.save(declaracion));
    }

    public List<DeclaracionImpuestoDto> listar(Long empresaId, Integer anio) {
        int anioEfectivo = anio == null ? LocalDate.now().getYear() : anio;
        return declaracionRepository
                .findByEmpresaIdAndAnioGravableOrderByImpuestoCodigoAscPeriodoAsc(empresaId, anioEfectivo)
                .stream()
                .map(this::mapear)
                .toList();
    }

    public DeclaracionImpuestoDto marcarPresentada(Long empresaId, Long declaracionId) {
        DeclaracionImpuestoJpa declaracion = declaracionRepository.findById(declaracionId)
                .filter(registro -> registro.getEmpresaId().equals(empresaId))
                .orElseThrow(() -> new IllegalArgumentException("Declaración no encontrada."));
        declaracion.setEstado("PRESENTADA");
        return mapear(declaracionRepository.save(declaracion));
    }

    public List<CalendarioTributarioDto> listarCalendario(Integer anio) {
        int anioEfectivo = anio == null ? LocalDate.now().getYear() : anio;
        return calendarioRepository.findByAnioGravableOrderByImpuestoCodigoAscPeriodoAsc(anioEfectivo)
                .stream()
                .map(this::mapear)
                .toList();
    }

    private DeclaracionImpuestoDto mapear(DeclaracionImpuestoJpa declaracion) {
        return new DeclaracionImpuestoDto(
                declaracion.getId(),
                declaracion.getEmpresaId(),
                declaracion.getImpuestoCodigo(),
                declaracion.getAnioGravable(),
                declaracion.getPeriodo(),
                declaracion.getFechaDesde(),
                declaracion.getFechaHasta(),
                declaracion.getFechaLimite(),
                declaracion.getEstado(),
                declaracion.getTotalCausado(),
                declaracion.getTotalPagado(),
                declaracion.getSaldo());
    }

    private CalendarioTributarioDto mapear(CalendarioTributarioJpa calendario) {
        return new CalendarioTributarioDto(
                calendario.getId(),
                calendario.getImpuestoCodigo(),
                calendario.getAnioGravable(),
                calendario.getPeriodicidad(),
                calendario.getPeriodo(),
                calendario.getFechaLimite());
    }

    private String requerir(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
