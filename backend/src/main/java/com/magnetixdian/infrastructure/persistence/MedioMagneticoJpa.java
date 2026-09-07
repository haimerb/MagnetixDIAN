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

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "medio_magnetico")
public class MedioMagneticoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaJpa empresa;

    @Column(nullable = false, length = 10)
    private String formato;

    @Column(name = "version_formato", nullable = false, length = 10)
    private String versionFormato = "10";

    @Column(name = "anio_gravable", nullable = false)
    private Integer anioGravable;

    @Column(nullable = false, length = 30)
    private String estado = "BORRADOR";

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MedioMagneticoJpa() {
    }

    public MedioMagneticoJpa(EmpresaJpa empresa, String formato, Integer anioGravable) {
        this.empresa = empresa;
        this.formato = formato;
        this.anioGravable = anioGravable;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public EmpresaJpa getEmpresa() {
        return empresa;
    }

    public String getFormato() {
        return formato;
    }

    public String getVersionFormato() {
        return versionFormato;
    }

    public Integer getAnioGravable() {
        return anioGravable;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setVersionFormato(String versionFormato) {
        this.versionFormato = versionFormato;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}