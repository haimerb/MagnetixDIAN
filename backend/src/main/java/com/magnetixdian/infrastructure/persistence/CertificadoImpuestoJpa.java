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
@Table(name = "certificado_impuesto")
public class CertificadoImpuestoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "tercero_id", nullable = false)
    private Long terceroId;

    @Column(name = "impuesto_codigo", nullable = false, length = 20)
    private String impuestoCodigo;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false, length = 40)
    private String numero;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public Long getTerceroId() {
        return terceroId;
    }

    public String getImpuestoCodigo() {
        return impuestoCodigo;
    }

    public Integer getAnio() {
        return anio;
    }

    public String getNumero() {
        return numero;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public void setTerceroId(Long terceroId) {
        this.terceroId = terceroId;
    }

    public void setImpuestoCodigo(String impuestoCodigo) {
        this.impuestoCodigo = impuestoCodigo;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }
}
