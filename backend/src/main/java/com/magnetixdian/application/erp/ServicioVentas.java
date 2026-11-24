package com.magnetixdian.application.erp;

import com.magnetixdian.domain.erp.evento.PedidoAprobadoEvent;
import com.magnetixdian.domain.erp.modelo.EstadoErp;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpOrdenVentaJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpOrdenVentaRepository;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpPedidoJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpPedidoRepository;
import com.magnetixdian.interfaces.dto.erp.OrdenVentaDto;
import com.magnetixdian.interfaces.dto.erp.OrdenVentaRequestDto;
import com.magnetixdian.interfaces.dto.erp.PedidoDto;
import com.magnetixdian.interfaces.dto.erp.PedidoRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class ServicioVentas {

    private final ErpOrdenVentaRepository ordenes;
    private final ErpPedidoRepository pedidos;
    private final PublicadorEventosErp publicador;

    public ServicioVentas(ErpOrdenVentaRepository ordenes, ErpPedidoRepository pedidos, PublicadorEventosErp publicador) {
        this.ordenes = ordenes;
        this.pedidos = pedidos;
        this.publicador = publicador;
    }

    public OrdenVentaDto crearOrden(Long empresaId, OrdenVentaRequestDto r) {
        validarMonto(r.total());
        ErpOrdenVentaJpa e = new ErpOrdenVentaJpa();
        e.setEmpresaId(empresaId);
        e.setNumero(requerido(r.numero(), "numero"));
        e.setClienteId(r.clienteId());
        e.setClienteNombre(r.clienteNombre());
        e.setFecha(r.fecha() == null ? LocalDate.now() : r.fecha());
        e.setEstado(EstadoErp.BORRADOR.name());
        e.setSubtotal(nvl(r.subtotal()));
        e.setImpuestos(nvl(r.impuestos()));
        e.setTotal(nvl(r.total()));
        e.setMoneda(r.moneda() == null || r.moneda().isBlank() ? "COP" : r.moneda().trim());
        e.setObservaciones(r.observaciones());
        return toDto(ordenes.save(e));
    }

    @Transactional(readOnly = true)
    public List<OrdenVentaDto> listarOrdenes(Long empresaId) {
        return ordenes.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream().map(this::toDto).toList();
    }

    public PedidoDto crearPedido(Long empresaId, PedidoRequestDto r) {
        validarMonto(r.total());
        if (r.ordenVentaId() != null) {
            ordenes.findByIdAndEmpresaId(r.ordenVentaId(), empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Orden de venta no encontrada"));
        }
        ErpPedidoJpa e = new ErpPedidoJpa();
        e.setEmpresaId(empresaId);
        e.setOrdenVentaId(r.ordenVentaId());
        e.setNumero(requerido(r.numero(), "numero"));
        e.setClienteId(r.clienteId());
        e.setClienteNombre(r.clienteNombre());
        e.setFecha(r.fecha() == null ? LocalDate.now() : r.fecha());
        e.setEstado(EstadoErp.BORRADOR.name());
        e.setSubtotal(nvl(r.subtotal()));
        e.setImpuestos(nvl(r.impuestos()));
        e.setTotal(nvl(r.total()));
        e.setObservaciones(r.observaciones());
        return toDto(pedidos.save(e));
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> listarPedidos(Long empresaId) {
        return pedidos.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream().map(this::toDto).toList();
    }

    public PedidoDto aprobarPedido(Long empresaId, Long pedidoId) {
        ErpPedidoJpa p = pedidos.findByIdAndEmpresaId(pedidoId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (EstadoErp.APROBADO.name().equals(p.getEstado())) {
            return toDto(p);
        }
        p.setEstado(EstadoErp.APROBADO.name());
        ErpPedidoJpa guardado = pedidos.save(p);
        publicador.publicar(new PedidoAprobadoEvent(empresaId, guardado.getId(), guardado.getNumero(),
                guardado.getClienteId(), guardado.getClienteNombre(), guardado.getFecha(),
                guardado.getSubtotal(), guardado.getImpuestos(), guardado.getTotal(), OffsetDateTime.now()));
        return toDto(guardado);
    }

    private String requerido(String v, String campo) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Campo requerido: " + campo);
        return v.trim();
    }

    private void validarMonto(BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Total inválido");
    }

    private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private OrdenVentaDto toDto(ErpOrdenVentaJpa e) {
        return new OrdenVentaDto(e.getId(), e.getEmpresaId(), e.getNumero(), e.getClienteId(), e.getClienteNombre(), e.getFecha(), e.getEstado(), e.getSubtotal(), e.getImpuestos(), e.getTotal(), e.getMoneda(), e.getObservaciones(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private PedidoDto toDto(ErpPedidoJpa e) {
        return new PedidoDto(e.getId(), e.getEmpresaId(), e.getOrdenVentaId(), e.getNumero(), e.getClienteId(), e.getClienteNombre(), e.getFecha(), e.getEstado(), e.getSubtotal(), e.getImpuestos(), e.getTotal(), e.getFacturaDianId(), e.getObservaciones(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
