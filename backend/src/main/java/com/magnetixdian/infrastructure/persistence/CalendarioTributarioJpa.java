package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "calendario_tributario")
public class CalendarioTributarioJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "impuesto_codigo", nullable = false, length = 20)
    private String impuestoCodigo;

    @Column(name = "anio_gravable", nullable = false)
    private Integer anioGravable;

    @Column(nullable = false, length = 15)
    private String periodicidad;

    @Column(nullable = false, length = 10)
    private String periodo;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    public Long getId() {
        return id;
    }

    public String getImpuestoCodigo() {
        return impuestoCodigo;
    }

    public Integer getAnioGravable() {
        return anioGravable;
    }

    public String getPeriodicidad() {
        return periodicidad;
    }

    public String getPeriodo() {
        return periodo;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }
}
