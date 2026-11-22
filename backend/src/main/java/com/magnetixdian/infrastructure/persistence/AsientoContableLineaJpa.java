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
@Table(name = "asiento_contable_linea")
public class AsientoContableLineaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_contable_id", nullable = false)
    private AsientoContableJpa asiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_contable_id", nullable = false)
    private CuentaContableJpa cuentaContable;

    @Column(name = "numero_linea", nullable = false)
    private int numeroLinea;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal debito = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal credito = BigDecimal.ZERO;

    @Column(name = "centro_costo_id")
    private Long centroCostoId;

    @Column(name = "tercero_id")
    private Long terceroId;

    @Column(name = "base_imponible", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseImponible = BigDecimal.ZERO;

    @Column(name = "impuesto_codigo", length = 20)
    private String impuestoCodigo;

    public Long getId() {
        return id;
    }

    public AsientoContableJpa getAsiento() {
        return asiento;
    }

    public void setAsiento(AsientoContableJpa asiento) {
        this.asiento = asiento;
    }

    public CuentaContableJpa getCuentaContable() {
        return cuentaContable;
    }

    public void setCuentaContable(CuentaContableJpa cuentaContable) {
        this.cuentaContable = cuentaContable;
    }

    public int getNumeroLinea() {
        return numeroLinea;
    }

    public void setNumeroLinea(int numeroLinea) {
        this.numeroLinea = numeroLinea;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getDebito() {
        return debito;
    }

    public void setDebito(BigDecimal debito) {
        this.debito = debito;
    }

    public BigDecimal getCredito() {
        return credito;
    }

    public void setCredito(BigDecimal credito) {
        this.credito = credito;
    }

    public Long getCentroCostoId() {
        return centroCostoId;
    }

    public void setCentroCostoId(Long centroCostoId) {
        this.centroCostoId = centroCostoId;
    }

    public Long getTerceroId() {
        return terceroId;
    }

    public void setTerceroId(Long terceroId) {
        this.terceroId = terceroId;
    }

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public String getImpuestoCodigo() {
        return impuestoCodigo;
    }

    public void setImpuestoCodigo(String impuestoCodigo) {
        this.impuestoCodigo = impuestoCodigo;
    }
}
