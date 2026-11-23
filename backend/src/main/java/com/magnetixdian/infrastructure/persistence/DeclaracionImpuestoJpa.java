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
@Table(name = "declaracion_impuesto")
public class DeclaracionImpuestoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "impuesto_codigo", nullable = false, length = 20)
    private String impuestoCodigo;

    @Column(name = "anio_gravable", nullable = false)
    private Integer anioGravable;

    @Column(nullable = false, length = 10)
    private String periodo;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(name = "total_causado", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCausado;

    @Column(name = "total_pagado", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPagado;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal saldo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getImpuestoCodigo() {
        return impuestoCodigo;
    }

    public Integer getAnioGravable() {
        return anioGravable;
    }

    public String getPeriodo() {
        return periodo;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public String getEstado() {
        return estado;
    }

    public BigDecimal getTotalCausado() {
        return totalCausado;
    }

    public BigDecimal getTotalPagado() {
        return totalPagado;
    }

    public BigDecimal getSaldo() {
        return saldo;
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

    public void setAnioGravable(Integer anioGravable) {
        this.anioGravable = anioGravable;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setTotalCausado(BigDecimal totalCausado) {
        this.totalCausado = totalCausado;
    }

    public void setTotalPagado(BigDecimal totalPagado) {
        this.totalPagado = totalPagado;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }
}
