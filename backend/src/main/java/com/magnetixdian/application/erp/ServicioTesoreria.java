package com.magnetixdian.application.erp;

import com.magnetixdian.application.ValidadorPeriodoAbierto;
import com.magnetixdian.domain.erp.evento.CobroClienteEvent;
import com.magnetixdian.domain.erp.evento.PagoProveedorEvent;
import com.magnetixdian.domain.erp.modelo.EstadoErp;
import com.magnetixdian.domain.erp.modelo.TipoMovimientoTesoreria;
import com.magnetixdian.infrastructure.integracion.erp.persistence.*;
import com.magnetixdian.interfaces.dto.erp.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class ServicioTesoreria {

    private final ErpCuentaBancariaRepository cuentas;
    private final ErpMovimientoTesoreriaRepository movimientos;
    private final ErpPagoProveedorRepository pagos;
    private final ErpCobroClienteRepository cobros;
    private final PublicadorEventosErp publicador;
    private final ValidadorPeriodoAbierto validadorPeriodo;

    public ServicioTesoreria(ErpCuentaBancariaRepository cuentas, ErpMovimientoTesoreriaRepository movimientos,
                             ErpPagoProveedorRepository pagos, ErpCobroClienteRepository cobros,
                             PublicadorEventosErp publicador, ValidadorPeriodoAbierto validadorPeriodo) {
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.pagos = pagos;
        this.cobros = cobros;
        this.publicador = publicador;
        this.validadorPeriodo = validadorPeriodo;
    }

    public CuentaBancariaDto crearCuenta(Long empresaId, CuentaBancariaRequestDto r) {
        if (r.banco() == null || r.banco().isBlank()) throw new IllegalArgumentException("Banco requerido");
        if (r.numeroCuenta() == null || r.numeroCuenta().isBlank()) throw new IllegalArgumentException("Número de cuenta requerido");
        ErpCuentaBancariaJpa e = new ErpCuentaBancariaJpa();
        e.setEmpresaId(empresaId);
        e.setBanco(r.banco().trim());
        e.setNumeroCuenta(r.numeroCuenta().trim());
        e.setTipoCuenta(r.tipoCuenta() == null || r.tipoCuenta().isBlank() ? "CORRIENTE" : r.tipoCuenta().trim());
        e.setMoneda(r.moneda() == null || r.moneda().isBlank() ? "COP" : r.moneda().trim());
        e.setSaldo(nvl(r.saldoInicial()));
        e.setActiva(r.activa() == null || r.activa());
        e.setEstado(e.isActiva() ? EstadoErp.ACTIVO.name() : EstadoErp.ANULADO.name());
        return toDto(cuentas.save(e));
    }

    @Transactional(readOnly = true)
    public List<CuentaBancariaDto> listarCuentas(Long empresaId) {
        return cuentas.findByEmpresaIdOrderByBancoAscNumeroCuentaAsc(empresaId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoTesoreriaDto> listarMovimientos(Long empresaId) {
        return movimientos.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream().map(this::toDto).toList();
    }

    public PagoProveedorDto registrarPagoProveedor(Long empresaId, PagoProveedorRequestDto r) {
        ErpCuentaBancariaJpa cuenta = cuentaActiva(empresaId, r.cuentaBancariaId());
        BigDecimal valor = monto(r.valor());
        LocalDate fecha = r.fecha() == null ? LocalDate.now() : r.fecha();
        validadorPeriodo.verificar(empresaId, fecha);
        if (cuenta.getSaldo().compareTo(valor) < 0) throw new IllegalArgumentException("Saldo insuficiente en la cuenta bancaria");
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.subtract(valor);
        cuenta.setSaldo(saldoNuevo);
        cuentas.save(cuenta);
        ErpPagoProveedorJpa e = new ErpPagoProveedorJpa();
        e.setEmpresaId(empresaId);
        e.setCompraId(r.compraId());
        e.setCuentaBancariaId(cuenta.getId());
        e.setProveedorId(r.proveedorId());
        e.setProveedorNombre(r.proveedorNombre());
        e.setFecha(fecha);
        e.setValor(valor);
        e.setMedioPago(r.medioPago());
        e.setReferencia(r.referencia());
        e.setEstado(EstadoErp.REGISTRADO.name());
        e.setObservaciones(r.observaciones());
        ErpPagoProveedorJpa guardado = pagos.save(e);
        guardarMovimiento(empresaId, cuenta.getId(), TipoMovimientoTesoreria.EGRESO, guardado.getFecha(), valor, saldoAnterior, saldoNuevo, "PAGO_PROVEEDOR", String.valueOf(guardado.getId()), "Pago proveedor " + nvlStr(guardado.getProveedorNombre()));
        publicador.publicar(new PagoProveedorEvent(empresaId, guardado.getId(), guardado.getCompraId(), guardado.getCuentaBancariaId(), guardado.getProveedorId(), guardado.getProveedorNombre(), guardado.getFecha(), guardado.getValor(), guardado.getReferencia(), OffsetDateTime.now()));
        return toDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<PagoProveedorDto> listarPagosProveedor(Long empresaId) {
        return pagos.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream().map(this::toDto).toList();
    }

    public CobroClienteDto registrarCobroCliente(Long empresaId, CobroClienteRequestDto r) {
        ErpCuentaBancariaJpa cuenta = cuentaActiva(empresaId, r.cuentaBancariaId());
        BigDecimal valor = monto(r.valor());
        LocalDate fecha = r.fecha() == null ? LocalDate.now() : r.fecha();
        validadorPeriodo.verificar(empresaId, fecha);
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.add(valor);
        cuenta.setSaldo(saldoNuevo);
        cuentas.save(cuenta);
        ErpCobroClienteJpa e = new ErpCobroClienteJpa();
        e.setEmpresaId(empresaId);
        e.setPedidoId(r.pedidoId());
        e.setCuentaBancariaId(cuenta.getId());
        e.setClienteId(r.clienteId());
        e.setClienteNombre(r.clienteNombre());
        e.setFecha(fecha);
        e.setValor(valor);
        e.setMedioPago(r.medioPago());
        e.setReferencia(r.referencia());
        e.setEstado(EstadoErp.REGISTRADO.name());
        e.setObservaciones(r.observaciones());
        ErpCobroClienteJpa guardado = cobros.save(e);
        guardarMovimiento(empresaId, cuenta.getId(), TipoMovimientoTesoreria.INGRESO, guardado.getFecha(), valor, saldoAnterior, saldoNuevo, "COBRO_CLIENTE", String.valueOf(guardado.getId()), "Cobro cliente " + nvlStr(guardado.getClienteNombre()));
        publicador.publicar(new CobroClienteEvent(empresaId, guardado.getId(), guardado.getPedidoId(), guardado.getCuentaBancariaId(), guardado.getClienteId(), guardado.getClienteNombre(), guardado.getFecha(), guardado.getValor(), guardado.getReferencia(), OffsetDateTime.now()));
        return toDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<CobroClienteDto> listarCobrosCliente(Long empresaId) {
        return cobros.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream().map(this::toDto).toList();
    }

    private void guardarMovimiento(Long empresaId, Long cuentaId, TipoMovimientoTesoreria tipo, LocalDate fecha, BigDecimal valor, BigDecimal saldoAnterior, BigDecimal saldoNuevo, String refTipo, String refId, String descripcion) {
        ErpMovimientoTesoreriaJpa m = new ErpMovimientoTesoreriaJpa();
        m.setEmpresaId(empresaId);
        m.setCuentaBancariaId(cuentaId);
        m.setTipo(tipo.name());
        m.setFecha(fecha);
        m.setValor(valor);
        m.setSaldoAnterior(saldoAnterior);
        m.setSaldoNuevo(saldoNuevo);
        m.setReferenciaTipo(refTipo);
        m.setReferenciaId(refId);
        m.setEstado(EstadoErp.REGISTRADO.name());
        m.setDescripcion(descripcion);
        movimientos.save(m);
    }

    private ErpCuentaBancariaJpa cuentaActiva(Long empresaId, Long cuentaId) {
        if (cuentaId == null) throw new IllegalArgumentException("Cuenta bancaria requerida");
        ErpCuentaBancariaJpa cuenta = cuentas.findByIdAndEmpresaId(cuentaId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta bancaria no encontrada"));
        if (!cuenta.isActiva()) throw new IllegalArgumentException("La cuenta bancaria no está activa");
        return cuenta;
    }

    private BigDecimal monto(BigDecimal v) { if (v == null || v.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Valor debe ser mayor a cero"); return v; }
    private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private String nvlStr(String v) { return v == null ? "" : v; }
    private CuentaBancariaDto toDto(ErpCuentaBancariaJpa e) { return new CuentaBancariaDto(e.getId(), e.getEmpresaId(), e.getBanco(), e.getNumeroCuenta(), e.getTipoCuenta(), e.getMoneda(), e.getSaldo(), e.isActiva(), e.getEstado(), e.getCreatedAt(), e.getUpdatedAt()); }
    private MovimientoTesoreriaDto toDto(ErpMovimientoTesoreriaJpa e) { return new MovimientoTesoreriaDto(e.getId(), e.getEmpresaId(), e.getCuentaBancariaId(), e.getTipo(), e.getFecha(), e.getValor(), e.getSaldoAnterior(), e.getSaldoNuevo(), e.getReferenciaTipo(), e.getReferenciaId(), e.getEstado(), e.getDescripcion(), e.getCreatedAt()); }
    private PagoProveedorDto toDto(ErpPagoProveedorJpa e) { return new PagoProveedorDto(e.getId(), e.getEmpresaId(), e.getCompraId(), e.getCuentaBancariaId(), e.getProveedorId(), e.getProveedorNombre(), e.getFecha(), e.getValor(), e.getMedioPago(), e.getReferencia(), e.getEstado(), e.getObservaciones(), e.getCreatedAt()); }
    private CobroClienteDto toDto(ErpCobroClienteJpa e) { return new CobroClienteDto(e.getId(), e.getEmpresaId(), e.getPedidoId(), e.getCuentaBancariaId(), e.getClienteId(), e.getClienteNombre(), e.getFecha(), e.getValor(), e.getMedioPago(), e.getReferencia(), e.getEstado(), e.getObservaciones(), e.getCreatedAt()); }
}
