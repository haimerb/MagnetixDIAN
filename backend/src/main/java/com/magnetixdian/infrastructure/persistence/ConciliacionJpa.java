package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conciliacion")
public class ConciliacionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "fecha_desde")
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta")
    private LocalDate fechaHasta;

    @Column(name = "total_documentos", nullable = false)
    private int totalDocumentos;

    @Column(nullable = false)
    private int coincidencias;

    @Column(nullable = false)
    private int diferencias;

    @Column(name = "documentos_sin_asiento", nullable = false)
    private int documentosSinAsiento;

    @Column(name = "valor_diferencia", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorDiferencia = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "conciliacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ConciliacionLineaJpa> lineas = new ArrayList<>();

    public void agregarLinea(ConciliacionLineaJpa linea) {
        linea.setConciliacion(this);
        this.lineas.add(linea);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public int getTotalDocumentos() {
        return totalDocumentos;
    }

    public void setTotalDocumentos(int totalDocumentos) {
        this.totalDocumentos = totalDocumentos;
    }

    public int getCoincidencias() {
        return coincidencias;
    }

    public void setCoincidencias(int coincidencias) {
        this.coincidencias = coincidencias;
    }

    public int getDiferencias() {
        return diferencias;
    }

    public void setDiferencias(int diferencias) {
        this.diferencias = diferencias;
    }

    public int getDocumentosSinAsiento() {
        return documentosSinAsiento;
    }

    public void setDocumentosSinAsiento(int documentosSinAsiento) {
        this.documentosSinAsiento = documentosSinAsiento;
    }

    public BigDecimal getValorDiferencia() {
        return valorDiferencia;
    }

    public void setValorDiferencia(BigDecimal valorDiferencia) {
        this.valorDiferencia = valorDiferencia;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ConciliacionLineaJpa> getLineas() {
        return lineas;
    }
}
