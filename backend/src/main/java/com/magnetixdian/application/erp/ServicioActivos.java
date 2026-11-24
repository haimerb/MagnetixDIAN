package com.magnetixdian.application.erp;

import com.magnetixdian.domain.erp.evento.DepreciacionCalculadaEvent;
import com.magnetixdian.domain.erp.modelo.EstadoErp;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpActivoJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpActivoRepository;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpDepreciacionJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpDepreciacionRepository;
import com.magnetixdian.interfaces.dto.erp.ActivoDto;
import com.magnetixdian.interfaces.dto.erp.ActivoRequestDto;
import com.magnetixdian.interfaces.dto.erp.BajaActivoRequestDto;
import com.magnetixdian.interfaces.dto.erp.DepreciacionDto;
import com.magnetixdian.interfaces.dto.erp.DepreciacionRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class ServicioActivos {

    private final ErpActivoRepository activos;
    private final ErpDepreciacionRepository depreciaciones;
    private final PublicadorEventosErp publicador;

    public ServicioActivos(ErpActivoRepository activos, ErpDepreciacionRepository depreciaciones, PublicadorEventosErp publicador) {
        this.activos = activos;
        this.depreciaciones = depreciaciones;
        this.publicador = publicador;
    }

    public ActivoDto darAlta(Long empresaId, ActivoRequestDto r) {
        if (r.codigo() == null || r.codigo().isBlank()) throw new IllegalArgumentException("Código requerido");
        if (r.nombre() == null || r.nombre().isBlank()) throw new IllegalArgumentException("Nombre requerido");
        if (r.valorAdquisicion() == null || r.valorAdquisicion().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Valor de adquisición inválido");
        if (r.vidaUtilMeses() == null || r.vidaUtilMeses() <= 0) throw new IllegalArgumentException("Vida útil inválida");
        BigDecimal residual = r.valorResidual() == null ? BigDecimal.ZERO : r.valorResidual();
        if (residual.compareTo(BigDecimal.ZERO) < 0 || residual.compareTo(r.valorAdquisicion()) >= 0) throw new IllegalArgumentException("Valor residual inválido");
        ErpActivoJpa e = new ErpActivoJpa();
        e.setEmpresaId(empresaId);
        e.setCodigo(r.codigo().trim());
        e.setNombre(r.nombre().trim());
        e.setCategoria(r.categoria());
        e.setFechaAlta(r.fechaAlta() == null ? LocalDate.now() : r.fechaAlta());
        e.setValorAdquisicion(r.valorAdquisicion().setScale(2, RoundingMode.HALF_UP));
        e.setValorResidual(residual.setScale(2, RoundingMode.HALF_UP));
        e.setVidaUtilMeses(r.vidaUtilMeses());
        e.setDepreciacionAcumulada(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        e.setValorLibros(e.getValorAdquisicion());
        e.setEstado(EstadoErp.ACTIVO.name());
        e.setObservaciones(r.observaciones());
        return toDto(activos.save(e));
    }

    @Transactional(readOnly = true)
    public List<ActivoDto> listar(Long empresaId) {
        return activos.findByEmpresaIdOrderByIdDesc(empresaId).stream().map(this::toDto).toList();
    }

    public DepreciacionDto calcularDepreciacion(Long empresaId, Long activoId, DepreciacionRequestDto r) {
        ErpActivoJpa activo = activos.findByIdAndEmpresaId(activoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Activo no encontrado"));
        if (!EstadoErp.ACTIVO.name().equals(activo.getEstado())) throw new IllegalArgumentException("El activo no está activo");
        LocalDate fecha = r.fecha() == null ? LocalDate.now() : r.fecha();
        String periodo = r.periodo() == null || r.periodo().isBlank() ? YearMonth.from(fecha).toString() : r.periodo().trim();
        depreciaciones.findByActivoIdAndPeriodo(activoId, periodo).ifPresent(d -> { throw new IllegalArgumentException("La depreciación del periodo ya existe"); });
        BigDecimal base = activo.getValorAdquisicion().subtract(activo.getValorResidual());
        BigDecimal mensual = base.divide(BigDecimal.valueOf(activo.getVidaUtilMeses()), 2, RoundingMode.HALF_UP);
        BigDecimal pendiente = base.subtract(activo.getDepreciacionAcumulada());
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("El activo ya está totalmente depreciado");
        BigDecimal valor = mensual.min(pendiente).setScale(2, RoundingMode.HALF_UP);
        BigDecimal acumulada = activo.getDepreciacionAcumulada().add(valor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal libros = activo.getValorAdquisicion().subtract(acumulada).setScale(2, RoundingMode.HALF_UP);
        activo.setDepreciacionAcumulada(acumulada);
        activo.setValorLibros(libros);
        activos.save(activo);
        ErpDepreciacionJpa d = new ErpDepreciacionJpa();
        d.setEmpresaId(empresaId);
        d.setActivoId(activoId);
        d.setPeriodo(periodo);
        d.setFecha(fecha);
        d.setValor(valor);
        d.setDepreciacionAcumulada(acumulada);
        d.setValorLibros(libros);
        d.setEstado(EstadoErp.REGISTRADO.name());
        ErpDepreciacionJpa guardada = depreciaciones.save(d);
        publicador.publicar(new DepreciacionCalculadaEvent(empresaId, activoId, guardada.getId(), activo.getCodigo(), periodo, fecha, valor, acumulada, libros, OffsetDateTime.now()));
        return toDto(guardada, activo.getCodigo());
    }

    public ActivoDto darBaja(Long empresaId, Long activoId, BajaActivoRequestDto r) {
        ErpActivoJpa activo = activos.findByIdAndEmpresaId(activoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Activo no encontrado"));
        activo.setEstado(EstadoErp.BAJA.name());
        activo.setFechaBaja(r == null || r.fechaBaja() == null ? LocalDate.now() : r.fechaBaja());
        if (r != null && r.motivo() != null && !r.motivo().isBlank()) activo.setObservaciones(r.motivo());
        return toDto(activos.save(activo));
    }

    @Transactional(readOnly = true)
    public List<DepreciacionDto> listarDepreciaciones(Long empresaId) {
        return depreciaciones.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream()
                .map(d -> toDto(d, activos.findById(d.getActivoId()).map(ErpActivoJpa::getCodigo).orElse(null))).toList();
    }

    private ActivoDto toDto(ErpActivoJpa e) {
        return new ActivoDto(e.getId(), e.getEmpresaId(), e.getCodigo(), e.getNombre(), e.getCategoria(), e.getFechaAlta(), e.getValorAdquisicion(), e.getValorResidual(), e.getVidaUtilMeses(), e.getDepreciacionAcumulada(), e.getValorLibros(), e.getEstado(), e.getFechaBaja(), e.getObservaciones(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private DepreciacionDto toDto(ErpDepreciacionJpa d, String codigoActivo) {
        return new DepreciacionDto(d.getId(), d.getEmpresaId(), d.getActivoId(), codigoActivo, d.getPeriodo(), d.getFecha(), d.getValor(), d.getDepreciacionAcumulada(), d.getValorLibros(), d.getEstado(), d.getCreatedAt());
    }
}
