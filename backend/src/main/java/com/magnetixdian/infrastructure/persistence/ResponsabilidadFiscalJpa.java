package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "responsabilidad_fiscal")
public class ResponsabilidadFiscalJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "tercero_id")
    private Long terceroId;

    @Column(name = "impuesto_codigo", nullable = false, length = 20)
    private String impuestoCodigo;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false)
    private LocalDate desde;

    private LocalDate hasta;

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

    public String getCodigo() {
        return codigo;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
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

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }
}
