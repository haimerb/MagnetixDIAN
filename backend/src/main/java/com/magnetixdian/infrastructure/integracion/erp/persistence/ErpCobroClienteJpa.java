package com.magnetixdian.infrastructure.integracion.erp.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "erp_cobro_cliente")
public class ErpCobroClienteJpa extends ErpBaseJpa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "pedido_id")
    private Long pedidoId;
    @Column(name = "cuenta_bancaria_id", nullable = false)
    private Long cuentaBancariaId;
    @Column(name = "cliente_id", length = 80)
    private String clienteId;
    @Column(name = "cliente_nombre", length = 200)
    private String clienteNombre;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;
    @Column(name = "medio_pago", length = 40)
    private String medioPago;
    @Column(length = 120)
    private String referencia;
    @Column(length = 1000)
    private String observaciones;
    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
    public Long getCuentaBancariaId() { return cuentaBancariaId; }
    public void setCuentaBancariaId(Long cuentaBancariaId) { this.cuentaBancariaId = cuentaBancariaId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
