package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "impuesto_causacion")
public class ImpuestoCausacionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "impuesto_codigo", nullable = false, length = 20)
    private String impuestoCodigo;

    @Column(nullable = false, length = 30)
    private String origen;

    @Column(name = "origen_id", nullable = false)
    private Long origenId;

    @Column(name = "tercero_id")
    private Long terceroId;

    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "concepto_codigo", length = 10)
    private String conceptoCodigo;

    @Column(name = "base_imponible", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseImponible;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal tarifa;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(length = 300)
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getImpuestoCodigo() {
        return impuestoCodigo;
    }

    public String getOrigen() {
        return origen;
    }

    public Long getOrigenId() {
        return origenId;
    }

    public Long getTerceroId() {
        return terceroId;
    }

    public String getPeriodo() {
        return periodo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getConceptoCodigo() {
        return conceptoCodigo;
    }

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public BigDecimal getTarifa() {
        return tarifa;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getEstado() {
        return estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public void setImpuestoCodigo(String impuestoCodigo) {
        this.impuestoCodigo = impuestoCodigo;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public void setOrigenId(Long origenId) {
        this.origenId = origenId;
    }

    public void setTerceroId(Long terceroId) {
        this.terceroId = terceroId;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public void setConceptoCodigo(String conceptoCodigo) {
        this.conceptoCodigo = conceptoCodigo;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public void setTarifa(BigDecimal tarifa) {
        this.tarifa = tarifa;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
