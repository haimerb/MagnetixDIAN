package com.magnetixdian.infrastructure.integracion.erp.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "erp_recepcion_linea")
public class ErpRecepcionLineaJpa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepcion_id", nullable = false)
    private ErpRecepcionJpa recepcion;
    @Column(name = "producto_id", nullable = false, length = 80)
    private String productoId;
    @Column(length = 300)
    private String descripcion;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;
    @Column(name = "costo_unitario", nullable = false, precision = 18, scale = 4)
    private BigDecimal costoUnitario;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total;
    public Long getId() { return id; }
    public ErpRecepcionJpa getRecepcion() { return recepcion; }
    public void setRecepcion(ErpRecepcionJpa recepcion) { this.recepcion = recepcion; }
    public String getProductoId() { return productoId; }
    public void setProductoId(String productoId) { this.productoId = productoId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
