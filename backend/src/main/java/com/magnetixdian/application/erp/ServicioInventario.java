package com.magnetixdian.application.erp;

import com.magnetixdian.domain.erp.modelo.EstadoErp;
import com.magnetixdian.domain.erp.modelo.TipoMovimientoInventario;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpInventarioMovimientoJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpInventarioMovimientoRepository;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpInventarioSaldoJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpInventarioSaldoRepository;
import com.magnetixdian.interfaces.dto.erp.ExistenciaInventarioDto;
import com.magnetixdian.interfaces.dto.erp.MovimientoInventarioDto;
import com.magnetixdian.interfaces.dto.erp.MovimientoInventarioRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class ServicioInventario {

    private final ErpInventarioMovimientoRepository movimientos;
    private final ErpInventarioSaldoRepository saldos;

    public ServicioInventario(ErpInventarioMovimientoRepository movimientos, ErpInventarioSaldoRepository saldos) {
        this.movimientos = movimientos;
        this.saldos = saldos;
    }

    public MovimientoInventarioDto registrarMovimiento(Long empresaId, MovimientoInventarioRequestDto request) {
        validar(request);
        TipoMovimientoInventario tipo = TipoMovimientoInventario.valueOf(request.tipo().trim().toUpperCase());
        BigDecimal cantidad = escalarCantidad(request.cantidad());
        BigDecimal costoUnitario = escalarCosto(request.costoUnitario() == null ? BigDecimal.ZERO : request.costoUnitario());
        ErpInventarioSaldoJpa saldo = saldos.findByEmpresaIdAndBodegaAndProductoId(empresaId, request.bodega(), request.productoId())
                .orElseGet(() -> nuevoSaldo(empresaId, request.bodega(), request.productoId()));
        BigDecimal existenciaAnterior = escalarCantidad(saldo.getExistencia());
        BigDecimal costoPromedioAnterior = escalarCosto(saldo.getCostoPromedio());
        BigDecimal existenciaNueva;
        BigDecimal costoPromedioNuevo;
        if (tipo == TipoMovimientoInventario.ENTRADA) {
            existenciaNueva = existenciaAnterior.add(cantidad);
            BigDecimal valorAnterior = existenciaAnterior.multiply(costoPromedioAnterior);
            BigDecimal valorEntrada = cantidad.multiply(costoUnitario);
            costoPromedioNuevo = existenciaNueva.signum() <= 0 ? BigDecimal.ZERO : valorAnterior.add(valorEntrada).divide(existenciaNueva, 4, RoundingMode.HALF_UP);
        } else {
            if (cantidad.compareTo(existenciaAnterior) > 0) {
                throw new IllegalArgumentException("Existencias insuficientes para la salida");
            }
            existenciaNueva = existenciaAnterior.subtract(cantidad);
            costoPromedioNuevo = costoPromedioAnterior;
            if (request.costoUnitario() == null || request.costoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                costoUnitario = costoPromedioAnterior;
            }
        }
        saldo.setExistencia(escalarCantidad(existenciaNueva));
        saldo.setCostoPromedio(escalarCosto(costoPromedioNuevo));
        saldo.setUpdatedAt(OffsetDateTime.now());
        saldos.save(saldo);

        ErpInventarioMovimientoJpa entidad = new ErpInventarioMovimientoJpa();
        entidad.setEmpresaId(empresaId);
        entidad.setTipo(tipo.name());
        entidad.setBodega(request.bodega().trim());
        entidad.setProductoId(request.productoId().trim());
        entidad.setDescripcion(request.descripcion());
        entidad.setCantidad(cantidad);
        entidad.setCostoUnitario(escalarCosto(costoUnitario));
        entidad.setCostoPromedio(escalarCosto(costoPromedioNuevo));
        entidad.setExistenciaAnterior(existenciaAnterior);
        entidad.setExistenciaNueva(escalarCantidad(existenciaNueva));
        entidad.setFecha(request.fecha() == null ? LocalDate.now() : request.fecha());
        entidad.setReferenciaTipo(request.referenciaTipo());
        entidad.setReferenciaId(request.referenciaId());
        entidad.setEstado(EstadoErp.REGISTRADO.name());
        entidad.setObservaciones(request.observaciones());
        return toDto(movimientos.save(entidad));
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDto> listarMovimientos(Long empresaId, String bodega) {
        List<ErpInventarioMovimientoJpa> lista = bodega == null || bodega.isBlank()
                ? movimientos.findByEmpresaIdOrderByFechaDescIdDesc(empresaId)
                : movimientos.findByEmpresaIdAndBodegaOrderByFechaDescIdDesc(empresaId, bodega.trim());
        return lista.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ExistenciaInventarioDto> listarExistencias(Long empresaId, String bodega) {
        List<ErpInventarioSaldoJpa> lista = bodega == null || bodega.isBlank()
                ? saldos.findByEmpresaIdOrderByBodegaAscProductoIdAsc(empresaId)
                : saldos.findByEmpresaIdAndBodegaOrderByProductoIdAsc(empresaId, bodega.trim());
        return lista.stream().map(s -> new ExistenciaInventarioDto(s.getEmpresaId(), s.getBodega(), s.getProductoId(), s.getExistencia(), s.getCostoPromedio(), s.getUpdatedAt())).toList();
    }

    private void validar(MovimientoInventarioRequestDto request) {
        if (request == null) throw new IllegalArgumentException("Solicitud requerida");
        if (request.tipo() == null || request.tipo().isBlank()) throw new IllegalArgumentException("Tipo de movimiento requerido");
        try { TipoMovimientoInventario.valueOf(request.tipo().trim().toUpperCase()); }
        catch (Exception ex) { throw new IllegalArgumentException("Tipo de movimiento inválido"); }
        if (request.bodega() == null || request.bodega().isBlank()) throw new IllegalArgumentException("Bodega requerida");
        if (request.productoId() == null || request.productoId().isBlank()) throw new IllegalArgumentException("Producto requerido");
        if (request.cantidad() == null || request.cantidad().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Cantidad debe ser mayor a cero");
        if ("ENTRADA".equalsIgnoreCase(request.tipo()) && (request.costoUnitario() == null || request.costoUnitario().compareTo(BigDecimal.ZERO) < 0)) {
            throw new IllegalArgumentException("Costo unitario inválido");
        }
    }

    private ErpInventarioSaldoJpa nuevoSaldo(Long empresaId, String bodega, String productoId) {
        ErpInventarioSaldoJpa saldo = new ErpInventarioSaldoJpa();
        saldo.setEmpresaId(empresaId);
        saldo.setBodega(bodega.trim());
        saldo.setProductoId(productoId.trim());
        saldo.setExistencia(BigDecimal.ZERO);
        saldo.setCostoPromedio(BigDecimal.ZERO);
        return saldo;
    }

    private MovimientoInventarioDto toDto(ErpInventarioMovimientoJpa e) {
        return new MovimientoInventarioDto(e.getId(), e.getEmpresaId(), e.getTipo(), e.getBodega(), e.getProductoId(), e.getDescripcion(), e.getCantidad(), e.getCostoUnitario(), e.getCostoPromedio(), e.getExistenciaAnterior(), e.getExistenciaNueva(), e.getFecha(), e.getReferenciaTipo(), e.getReferenciaId(), e.getEstado(), e.getObservaciones(), e.getCreatedAt());
    }

    private BigDecimal escalarCantidad(BigDecimal v) { return v.setScale(4, RoundingMode.HALF_UP); }
    private BigDecimal escalarCosto(BigDecimal v) { return v.setScale(4, RoundingMode.HALF_UP); }
}
