package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "factura_linea")
public class FacturaLineaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private FacturaJpa factura;

    @Column(name = "numero_linea", nullable = false)
    private int numeroLinea;

    @Column(name = "codigo_producto", length = 50)
    private String codigoProducto;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad = BigDecimal.ONE;

    @Column(name = "unidad_medida", nullable = false, length = 10)
    private String unidadMedida = "94";

    @Column(name = "valor_unitario", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorUnitario = BigDecimal.ZERO;

    @Column(name = "valor_descuento", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorDescuento = BigDecimal.ZERO;

    @Column(name = "iva_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal ivaPct = BigDecimal.ZERO;

    @Column(name = "iva_valor", nullable = false, precision = 18, scale = 2)
    private BigDecimal ivaValor = BigDecimal.ZERO;

    @Column(name = "inc_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal incPct = BigDecimal.ZERO;

    @Column(name = "inc_valor", nullable = false, precision = 18, scale = 2)
    private BigDecimal incValor = BigDecimal.ZERO;

    @Column(name = "total_linea", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalLinea = BigDecimal.ZERO;

    @Column(name = "codigo_unspsc", length = 20)
    private String codigoUnspsc;

    public Long getId() {
        return id;
    }

    public FacturaJpa getFactura() {
        return factura;
    }

    public void setFactura(FacturaJpa factura) {
        this.factura = factura;
    }

    public int getNumeroLinea() {
        return numeroLinea;
    }

    public void setNumeroLinea(int numeroLinea) {
        this.numeroLinea = numeroLinea;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getValorDescuento() {
        return valorDescuento;
    }

    public void setValorDescuento(BigDecimal valorDescuento) {
        this.valorDescuento = valorDescuento;
    }

    public BigDecimal getIvaPct() {
        return ivaPct;
    }

    public void setIvaPct(BigDecimal ivaPct) {
        this.ivaPct = ivaPct;
    }

    public BigDecimal getIvaValor() {
        return ivaValor;
    }

    public void setIvaValor(BigDecimal ivaValor) {
        this.ivaValor = ivaValor;
    }

    public BigDecimal getIncPct() {
        return incPct;
    }

    public void setIncPct(BigDecimal incPct) {
        this.incPct = incPct;
    }

    public BigDecimal getIncValor() {
        return incValor;
    }

    public void setIncValor(BigDecimal incValor) {
        this.incValor = incValor;
    }

    public BigDecimal getTotalLinea() {
        return totalLinea;
    }

    public void setTotalLinea(BigDecimal totalLinea) {
        this.totalLinea = totalLinea;
    }

    public String getCodigoUnspsc() {
        return codigoUnspsc;
    }

    public void setCodigoUnspsc(String codigoUnspsc) {
        this.codigoUnspsc = codigoUnspsc;
    }
}
