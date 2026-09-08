package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Calendario DIAN de vencimientos por año gravable y tipo de contribuyente.
 */
@Entity
@Table(name = "calendario_dian")
public class CalendarioDianJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anio_gravable", nullable = false)
    private Integer anioGravable;

    @Column(name = "anio_presentacion", nullable = false)
    private Integer anioPresentacion;

    @Column(name = "tipo_reporte", nullable = false, length = 30)
    private String tipoReporte;

    @Column(name = "rango_nit_ini", length = 2)
    private String rangoNitIni;

    @Column(name = "rango_nit_fin", length = 2)
    private String rangoNitFin;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    protected CalendarioDianJpa() {
    }

    public CalendarioDianJpa(Integer anioGravable, Integer anioPresentacion, String tipoReporte,
                             String rangoNitIni, String rangoNitFin, LocalDate fechaLimite) {
        this.anioGravable = anioGravable;
        this.anioPresentacion = anioPresentacion;
        this.tipoReporte = tipoReporte;
        this.rangoNitIni = rangoNitIni;
        this.rangoNitFin = rangoNitFin;
        this.fechaLimite = fechaLimite;
    }

    public Long getId() {
        return id;
    }

    public Integer getAnioGravable() {
        return anioGravable;
    }

    public Integer getAnioPresentacion() {
        return anioPresentacion;
    }

    public String getTipoReporte() {
        return tipoReporte;
    }

    public String getRangoNitIni() {
        return rangoNitIni;
    }

    public String getRangoNitFin() {
        return rangoNitFin;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }
}