package com.magnetixdian.infrastructure.integracion.erp.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "erp_pedido")
public class ErpPedidoJpa extends ErpBaseJpa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "orden_venta_id")
    private Long ordenVentaId;
    @Column(nullable = false, length = 40)
    private String numero;
    @Column(name = "cliente_id", length = 80)
    private String clienteId;
    @Column(name = "cliente_nombre", length = 200)
    private String clienteNombre;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal impuestos = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;
    @Column(name = "factura_dian_id", length = 120)
    private String facturaDianId;
    @Column(length = 1000)
    private String observaciones;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrdenVentaId() { return ordenVentaId; }
    public void setOrdenVentaId(Long ordenVentaId) { this.ordenVentaId = ordenVentaId; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getImpuestos() { return impuestos; }
    public void setImpuestos(BigDecimal impuestos) { this.impuestos = impuestos; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getFacturaDianId() { return facturaDianId; }
    public void setFacturaDianId(String facturaDianId) { this.facturaDianId = facturaDianId; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
