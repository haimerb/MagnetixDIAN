package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "uvt_vigencia")
public class UvtVigenciaJpa {

    @Id
    private Integer anio;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    private String resolucion;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    protected UvtVigenciaJpa() {
    }

    public UvtVigenciaJpa(Integer anio, BigDecimal valor, String resolucion) {
        this.anio = anio;
        this.valor = valor;
        this.resolucion = resolucion;
    }

    public Integer getAnio() {
        return anio;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getResolucion() {
        return resolucion;
    }

    public Boolean getActivo() {
        return activo;
    }
}