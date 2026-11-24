package com.magnetixdian.application.erp;

import com.magnetixdian.domain.erp.evento.CompraRecibidaEvent;
import com.magnetixdian.domain.erp.modelo.EstadoErp;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpCompraJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpCompraRepository;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpRecepcionJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpRecepcionLineaJpa;
import com.magnetixdian.infrastructure.integracion.erp.persistence.ErpRecepcionRepository;
import com.magnetixdian.interfaces.dto.erp.CompraDto;
import com.magnetixdian.interfaces.dto.erp.CompraRequestDto;
import com.magnetixdian.interfaces.dto.erp.MovimientoInventarioRequestDto;
import com.magnetixdian.interfaces.dto.erp.RecepcionDto;
import com.magnetixdian.interfaces.dto.erp.RecepcionLineaDto;
import com.magnetixdian.interfaces.dto.erp.RecepcionLineaRequestDto;
import com.magnetixdian.interfaces.dto.erp.RecepcionRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class ServicioCompras {

    private final ErpCompraRepository compras;
    private final ErpRecepcionRepository recepciones;
    private final ServicioInventario inventario;
    private final PublicadorEventosErp publicador;

    public ServicioCompras(ErpCompraRepository compras, ErpRecepcionRepository recepciones, ServicioInventario inventario, PublicadorEventosErp publicador) {
        this.compras = compras;
        this.recepciones = recepciones;
        this.inventario = inventario;
        this.publicador = publicador;
    }

    public CompraDto crearOrdenCompra(Long empresaId, CompraRequestDto r) {
        if (r.total() == null || r.total().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Total inválido");
        ErpCompraJpa e = new ErpCompraJpa();
        e.setEmpresaId(empresaId);
        e.setNumero(req(r.numero(), "numero"));
        e.setProveedorId(r.proveedorId());
        e.setProveedorNombre(r.proveedorNombre());
        e.setFecha(r.fecha() == null ? LocalDate.now() : r.fecha());
        e.setEstado(EstadoErp.BORRADOR.name());
        e.setSubtotal(nvl(r.subtotal()));
        e.setImpuestos(nvl(r.impuestos()));
        e.setTotal(nvl(r.total()));
        e.setObservaciones(r.observaciones());
        return toDto(compras.save(e));
    }

    @Transactional(readOnly = true)
    public List<CompraDto> listarCompras(Long empresaId) {
        return compras.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream().map(this::toDto).toList();
    }

    public RecepcionDto recibirCompra(Long empresaId, Long compraId, RecepcionRequestDto r) {
        ErpCompraJpa compra = compras.findByIdAndEmpresaId(compraId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));
        if (r.lineas() == null || r.lineas().isEmpty()) throw new IllegalArgumentException("La recepción debe tener líneas");
        ErpRecepcionJpa rec = new ErpRecepcionJpa();
        rec.setEmpresaId(empresaId);
        rec.setCompraId(compraId);
        rec.setNumero(req(r.numero(), "numero"));
        rec.setFecha(r.fecha() == null ? LocalDate.now() : r.fecha());
        rec.setBodega(req(r.bodega(), "bodega"));
        rec.setEstado(EstadoErp.RECIBIDA.name());
        rec.setObservaciones(r.observaciones());
        BigDecimal total = BigDecimal.ZERO;
        for (RecepcionLineaRequestDto l : r.lineas()) {
            if (l.productoId() == null || l.productoId().isBlank()) throw new IllegalArgumentException("Producto requerido en línea");
            if (l.cantidad() == null || l.cantidad().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Cantidad inválida");
            if (l.costoUnitario() == null || l.costoUnitario().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Costo inválido");
            ErpRecepcionLineaJpa linea = new ErpRecepcionLineaJpa();
            linea.setRecepcion(rec);
            linea.setProductoId(l.productoId().trim());
            linea.setDescripcion(l.descripcion());
            linea.setCantidad(l.cantidad());
            linea.setCostoUnitario(l.costoUnitario());
            BigDecimal lineaTotal = l.cantidad().multiply(l.costoUnitario()).setScale(2, RoundingMode.HALF_UP);
            linea.setTotal(lineaTotal);
            rec.getLineas().add(linea);
            total = total.add(lineaTotal);
        }
        rec.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        ErpRecepcionJpa guardada = recepciones.save(rec);
        compra.setEstado(EstadoErp.RECIBIDA.name());
        compras.save(compra);
        for (ErpRecepcionLineaJpa l : guardada.getLineas()) {
            inventario.registrarMovimiento(empresaId, new MovimientoInventarioRequestDto("ENTRADA", guardada.getBodega(), l.getProductoId(), l.getDescripcion(), l.getCantidad(), l.getCostoUnitario(), guardada.getFecha(), "RECEPCION_COMPRA", String.valueOf(guardada.getId()), "Recepción de compra " + compra.getNumero()));
        }
        publicador.publicar(new CompraRecibidaEvent(empresaId, compra.getId(), guardada.getId(), compra.getNumero(), compra.getProveedorId(), compra.getProveedorNombre(), guardada.getBodega(), guardada.getFecha(), guardada.getTotal(), OffsetDateTime.now()));
        return toDto(guardada);
    }

    @Transactional(readOnly = true)
    public List<RecepcionDto> listarRecepciones(Long empresaId) {
        return recepciones.findByEmpresaIdOrderByFechaDescIdDesc(empresaId).stream().map(this::toDto).toList();
    }

    private String req(String v, String campo) { if (v == null || v.isBlank()) throw new IllegalArgumentException("Campo requerido: " + campo); return v.trim(); }
    private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private CompraDto toDto(ErpCompraJpa e) { return new CompraDto(e.getId(), e.getEmpresaId(), e.getNumero(), e.getProveedorId(), e.getProveedorNombre(), e.getFecha(), e.getEstado(), e.getSubtotal(), e.getImpuestos(), e.getTotal(), e.getObservaciones(), e.getCreatedAt(), e.getUpdatedAt()); }
    private RecepcionDto toDto(ErpRecepcionJpa e) {
        List<RecepcionLineaDto> lineas = e.getLineas().stream().map(l -> new RecepcionLineaDto(l.getId(), l.getProductoId(), l.getDescripcion(), l.getCantidad(), l.getCostoUnitario(), l.getTotal())).toList();
        return new RecepcionDto(e.getId(), e.getEmpresaId(), e.getCompraId(), e.getNumero(), e.getFecha(), e.getBodega(), e.getEstado(), e.getTotal(), e.getObservaciones(), e.getCreatedAt(), e.getUpdatedAt(), lineas);
    }
}
