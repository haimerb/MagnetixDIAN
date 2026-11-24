package com.magnetixdian.infrastructure.integracion.erp.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "erp_inventario_movimiento")
public class ErpInventarioMovimientoJpa extends ErpBaseJpa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String tipo;
    @Column(nullable = false, length = 80)
    private String bodega;
    @Column(name = "producto_id", nullable = false, length = 80)
    private String productoId;
    @Column(length = 300)
    private String descripcion;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal cantidad;
    @Column(name = "costo_unitario", nullable = false, precision = 18, scale = 4)
    private BigDecimal costoUnitario;
    @Column(name = "costo_promedio", nullable = false, precision = 18, scale = 4)
    private BigDecimal costoPromedio;
    @Column(name = "existencia_anterior", nullable = false, precision = 18, scale = 4)
    private BigDecimal existenciaAnterior;
    @Column(name = "existencia_nueva", nullable = false, precision = 18, scale = 4)
    private BigDecimal existenciaNueva;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(name = "referencia_tipo", length = 40)
    private String referenciaTipo;
    @Column(name = "referencia_id", length = 80)
    private String referenciaId;
    @Column(length = 1000)
    private String observaciones;
    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getBodega() { return bodega; }
    public void setBodega(String bodega) { this.bodega = bodega; }
    public String getProductoId() { return productoId; }
    public void setProductoId(String productoId) { this.productoId = productoId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public BigDecimal getCostoPromedio() { return costoPromedio; }
    public void setCostoPromedio(BigDecimal costoPromedio) { this.costoPromedio = costoPromedio; }
    public BigDecimal getExistenciaAnterior() { return existenciaAnterior; }
    public void setExistenciaAnterior(BigDecimal existenciaAnterior) { this.existenciaAnterior = existenciaAnterior; }
    public BigDecimal getExistenciaNueva() { return existenciaNueva; }
    public void setExistenciaNueva(BigDecimal existenciaNueva) { this.existenciaNueva = existenciaNueva; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public void setReferenciaTipo(String referenciaTipo) { this.referenciaTipo = referenciaTipo; }
    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
