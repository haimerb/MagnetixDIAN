package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "impuesto_tarifa")
public class ImpuestoTarifaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "impuesto_codigo", nullable = false, length = 20)
    private String impuestoCodigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer anio;

    @Column(name = "concepto_codigo", length = 10)
    private String conceptoCodigo;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal porcentaje;

    @Column(name = "base_min_uvt", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseMinUvt;

    @Column(name = "base_max_uvt", precision = 12, scale = 2)
    private BigDecimal baseMaxUvt;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public String getImpuestoCodigo() {
        return impuestoCodigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Integer getAnio() {
        return anio;
    }

    public String getConceptoCodigo() {
        return conceptoCodigo;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public BigDecimal getBaseMinUvt() {
        return baseMinUvt;
    }

    public BigDecimal getBaseMaxUvt() {
        return baseMaxUvt;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setImpuestoCodigo(String impuestoCodigo) {
        this.impuestoCodigo = impuestoCodigo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public void setConceptoCodigo(String conceptoCodigo) {
        this.conceptoCodigo = conceptoCodigo;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }

    public void setBaseMinUvt(BigDecimal baseMinUvt) {
        this.baseMinUvt = baseMinUvt;
    }

    public void setBaseMaxUvt(BigDecimal baseMaxUvt) {
        this.baseMaxUvt = baseMaxUvt;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
