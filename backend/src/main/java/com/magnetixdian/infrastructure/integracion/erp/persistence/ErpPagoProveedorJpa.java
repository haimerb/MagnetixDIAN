package com.magnetixdian.infrastructure.integracion.erp.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "erp_pago_proveedor")
public class ErpPagoProveedorJpa extends ErpBaseJpa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "compra_id")
    private Long compraId;
    @Column(name = "cuenta_bancaria_id", nullable = false)
    private Long cuentaBancariaId;
    @Column(name = "proveedor_id", length = 80)
    private String proveedorId;
    @Column(name = "proveedor_nombre", length = 200)
    private String proveedorNombre;
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
    public Long getCompraId() { return compraId; }
    public void setCompraId(Long compraId) { this.compraId = compraId; }
    public Long getCuentaBancariaId() { return cuentaBancariaId; }
    public void setCuentaBancariaId(Long cuentaBancariaId) { this.cuentaBancariaId = cuentaBancariaId; }
    public String getProveedorId() { return proveedorId; }
    public void setProveedorId(String proveedorId) { this.proveedorId = proveedorId; }
    public String getProveedorNombre() { return proveedorNombre; }
    public void setProveedorNombre(String proveedorNombre) { this.proveedorNombre = proveedorNombre; }
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
