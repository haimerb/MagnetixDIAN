package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asiento_contable")
public class AsientoContableJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "documento_tipo", nullable = false, length = 30)
    private String documentoTipo;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @Column(name = "tipo_evento", nullable = false, length = 30)
    private String tipoEvento;

    @Column(nullable = false, length = 15)
    private String estado = "CONTABILIZADO";

    @Column(name = "total_debito", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDebito = BigDecimal.ZERO;

    @Column(name = "total_credito", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCredito = BigDecimal.ZERO;

    @Column(name = "valor_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "valor_impuestos", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorImpuestos = BigDecimal.ZERO;

    @Column(name = "valor_retenciones", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorRetenciones = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numeroLinea ASC")
    private List<AsientoContableLineaJpa> lineas = new ArrayList<>();

    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DocumentoAsientoJpa> documentos = new ArrayList<>();

    public void agregarLinea(AsientoContableLineaJpa linea) {
        linea.setAsiento(this);
        this.lineas.add(linea);
    }

    public void agregarDocumento(DocumentoAsientoJpa documento) {
        documento.setAsiento(this);
        this.documentos.add(documento);
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

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDocumentoTipo() {
        return documentoTipo;
    }

    public void setDocumentoTipo(String documentoTipo) {
        this.documentoTipo = documentoTipo;
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getTotalDebito() {
        return totalDebito;
    }

    public void setTotalDebito(BigDecimal totalDebito) {
        this.totalDebito = totalDebito;
    }

    public BigDecimal getTotalCredito() {
        return totalCredito;
    }

    public void setTotalCredito(BigDecimal totalCredito) {
        this.totalCredito = totalCredito;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorImpuestos() {
        return valorImpuestos;
    }

    public void setValorImpuestos(BigDecimal valorImpuestos) {
        this.valorImpuestos = valorImpuestos;
    }

    public BigDecimal getValorRetenciones() {
        return valorRetenciones;
    }

    public void setValorRetenciones(BigDecimal valorRetenciones) {
        this.valorRetenciones = valorRetenciones;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<AsientoContableLineaJpa> getLineas() {
        return lineas;
    }

    public List<DocumentoAsientoJpa> getDocumentos() {
        return documentos;
    }
}
