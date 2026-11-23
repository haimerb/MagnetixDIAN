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
@Table(name = "impuesto_pago")
public class ImpuestoPagoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "causacion_id", nullable = false)
    private Long causacionId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(length = 50)
    private String comprobante;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public Long getCausacionId() {
        return causacionId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getComprobante() {
        return comprobante;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public void setCausacionId(Long causacionId) {
        this.causacionId = causacionId;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public void setComprobante(String comprobante) {
        this.comprobante = comprobante;
    }
}
