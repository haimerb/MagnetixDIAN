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
import java.time.OffsetDateTime;

@Entity
@Table(name = "operacion")
public class OperacionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medio_magnetico_id", nullable = false)
    private MedioMagneticoJpa medioMagnetico;

    @Column(nullable = false)
    private Integer linea;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    private String tipoDocumento;

    @Column(name = "numero_identificacion", nullable = false, length = 20)
    private String numeroIdentificacion;

    @Column
    private Integer dv;

    @Column(nullable = false, length = 10)
    private String concepto;

    @Column(name = "valor_pago", precision = 18, scale = 2, nullable = false)
    private BigDecimal valorPago;

    @Column(name = "retencion_renta", precision = 18, scale = 2)
    private BigDecimal retencionRenta;

    @Column(name = "retencion_iva", precision = 18, scale = 2)
    private BigDecimal retencionIva;

    @Column(name = "retencion_ica", precision = 18, scale = 2)
    private BigDecimal retencionIca;

    @Column(name = "retencion_timbre", precision = 18, scale = 2)
    private BigDecimal retencionTimbre;

    @Column(name = "iva_pagado", precision = 18, scale = 2)
    private BigDecimal ivaPagado;

    @Column(name = "iva_descontable", precision = 18, scale = 2)
    private BigDecimal ivaDescontable;

    @Column(name = "valor_gasto", precision = 18, scale = 2)
    private BigDecimal valorGasto;

    @Column(name = "valor_costo", precision = 18, scale = 2)
    private BigDecimal valorCosto;

    @Column(name = "valor_nc", precision = 18, scale = 2)
    private BigDecimal valorNc;

    @Column(name = "cuantia_menor", nullable = false)
    private boolean cuantiaMenor;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public OperacionJpa() {
    }

    public Long getId() {
        return id;
    }

    public MedioMagneticoJpa getMedioMagnetico() {
        return medioMagnetico;
    }

    public Integer getLinea() {
        return linea;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public Integer getDv() {
        return dv;
    }

    public String getConcepto() {
        return concepto;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public BigDecimal getRetencionRenta() {
        return retencionRenta;
    }

    public BigDecimal getRetencionIva() {
        return retencionIva;
    }

    public BigDecimal getRetencionIca() {
        return retencionIca;
    }

    public BigDecimal getRetencionTimbre() {
        return retencionTimbre;
    }

    public BigDecimal getIvaPagado() {
        return ivaPagado;
    }

    public BigDecimal getIvaDescontable() {
        return ivaDescontable;
    }

    public BigDecimal getValorGasto() {
        return valorGasto;
    }

    public BigDecimal getValorCosto() {
        return valorCosto;
    }

    public BigDecimal getValorNc() {
        return valorNc;
    }

    public boolean isCuantiaMenor() {
        return cuantiaMenor;
    }

    public void persistir(MedioMagneticoJpa medioMagnetico, com.magnetixdian.domain.model.OperacionDatos datos) {
        this.medioMagnetico = medioMagnetico;
        this.linea = datos.linea();
        this.tipoDocumento = datos.tipoDocumento();
        this.numeroIdentificacion = datos.numeroIdentificacion();
        this.dv = datos.dv();
        this.concepto = datos.concepto();
        this.valorPago = datos.valorPago();
        this.retencionRenta = datos.retencionRenta();
        this.retencionIva = datos.retencionIva();
        this.retencionIca = datos.retencionIca();
        this.retencionTimbre = datos.retencionTimbre();
        this.ivaPagado = datos.ivaPagado();
        this.ivaDescontable = datos.ivaDescontable();
        this.valorGasto = datos.valorGasto();
        this.valorCosto = datos.valorCosto();
        this.valorNc = datos.valorNc();
        this.cuantiaMenor = datos.cuantiaMenor();
        this.createdAt = OffsetDateTime.now();
    }
}