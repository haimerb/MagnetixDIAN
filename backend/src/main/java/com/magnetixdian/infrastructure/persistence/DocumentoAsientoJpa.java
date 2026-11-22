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

import java.time.OffsetDateTime;

@Entity
@Table(name = "documento_asiento")
public class DocumentoAsientoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "documento_radian_id", nullable = false)
    private Long documentoRadianId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_contable_id", nullable = false)
    private AsientoContableJpa asiento;

    @Column(name = "tipo_evento", nullable = false, length = 30)
    private String tipoEvento;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getDocumentoRadianId() {
        return documentoRadianId;
    }

    public void setDocumentoRadianId(Long documentoRadianId) {
        this.documentoRadianId = documentoRadianId;
    }

    public AsientoContableJpa getAsiento() {
        return asiento;
    }

    public void setAsiento(AsientoContableJpa asiento) {
        this.asiento = asiento;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
