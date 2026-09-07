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

@Entity
@Table(name = "detalle_error")
public class DetalleErrorJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validacion_id", nullable = false)
    private ValidacionJpa validacion;

    @Column(name = "regla_id")
    private Long reglaId;

    @Column
    private Integer linea;

    @Column(length = 80)
    private String campo;

    @Column(name = "valor_esperado", length = 120)
    private String valorEsperado;

    @Column(name = "valor_encontrado", length = 120)
    private String valorEncontrado;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false, length = 10)
    private String severidad;

    public DetalleErrorJpa() {
    }

    public void validarEn(ValidacionJpa validacion) {
        this.validacion = validacion;
    }

    public void cargar(Long reglaId, Integer linea, String campo,
                       String valorEsperado, String valorEncontrado,
                       String mensaje, String severidad) {
        this.reglaId = reglaId;
        this.linea = linea;
        this.campo = campo;
        this.valorEsperado = valorEsperado;
        this.valorEncontrado = valorEncontrado;
        this.mensaje = mensaje;
        this.severidad = severidad;
    }

    public Long getId() {
        return id;
    }

    public Long getReglaId() {
        return reglaId;
    }

    public Integer getLinea() {
        return linea;
    }

    public String getCampo() {
        return campo;
    }

    public String getValorEsperado() {
        return valorEsperado;
    }

    public String getValorEncontrado() {
        return valorEncontrado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getSeveridad() {
        return severidad;
    }
}