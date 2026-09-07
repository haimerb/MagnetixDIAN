package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "validacion")
public class ValidacionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medio_magnetico_id", nullable = false)
    private MedioMagneticoJpa medioMagnetico;

    @Column(nullable = false)
    private OffsetDateTime inicio;

    private OffsetDateTime fin;

    @Column(nullable = false, length = 20)
    private String estado = "EN_PROCESO";

    @Column(name = "errores_total", nullable = false)
    private int erroresTotal;

    @Column(name = "advertencias_total", nullable = false)
    private int advertenciasTotal;

    @Column(name = "registros_validados", nullable = false)
    private int registrosValidados;

    @OneToMany(mappedBy = "validacion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleErrorJpa> detalles = new ArrayList<>();

    protected ValidacionJpa() {
    }

    public ValidacionJpa(MedioMagneticoJpa medioMagnetico) {
        this.medioMagnetico = medioMagnetico;
        this.inicio = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public MedioMagneticoJpa getMedioMagnetico() {
        return medioMagnetico;
    }

    public OffsetDateTime getInicio() {
        return inicio;
    }

    public OffsetDateTime getFin() {
        return fin;
    }

    public String getEstado() {
        return estado;
    }

    public int getErroresTotal() {
        return erroresTotal;
    }

    public int getAdvertenciasTotal() {
        return advertenciasTotal;
    }

    public int getRegistrosValidados() {
        return registrosValidados;
    }

    public List<DetalleErrorJpa> getDetalles() {
        return detalles;
    }

    public void completar(int errores, int advertencias, int registros) {
        this.erroresTotal = errores;
        this.advertenciasTotal = advertencias;
        this.registrosValidados = registros;
        this.estado = "COMPLETADA";
        this.fin = OffsetDateTime.now();
    }

    public void agregarDetalle(DetalleErrorJpa detalle) {
        detalle.validarEn(this);
        this.detalles.add(detalle);
    }
}