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
@Table(name = "documento_radian")
public class DocumentoRadianJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 10)
    private String direccion;

    @Column(name = "tipo_documento", nullable = false, length = 2)
    private String tipoDocumento;

    @Column(name = "cufe_cude", nullable = false, length = 200)
    private String cufeCude;

    @Column(nullable = false, length = 50)
    private String consecutivo;

    @Column(name = "nit_contraparte", length = 20)
    private String nitContraparte;

    @Column(name = "razon_social_contraparte", length = 300)
    private String razonSocialContraparte;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal impuestos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal retenciones = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado_radian", nullable = false, length = 30)
    private String estadoRadian = "PENDIENTE";

    @Column(name = "estado_causacion", nullable = false, length = 20)
    private String estadoCausacion = "PENDIENTE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numeroLinea ASC")
    private List<DocumentoRadianLineaJpa> lineas = new ArrayList<>();

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DocumentoRadianImpuestoJpa> impuestosDetalle = new ArrayList<>();

    public void agregarLinea(DocumentoRadianLineaJpa linea) {
        linea.setDocumento(this);
        this.lineas.add(linea);
    }

    public void agregarImpuesto(DocumentoRadianImpuestoJpa impuesto) {
        impuesto.setDocumento(this);
        this.impuestosDetalle.add(impuesto);
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getCufeCude() {
        return cufeCude;
    }

    public void setCufeCude(String cufeCude) {
        this.cufeCude = cufeCude;
    }

    public String getConsecutivo() {
        return consecutivo;
    }

    public void setConsecutivo(String consecutivo) {
        this.consecutivo = consecutivo;
    }

    public String getNitContraparte() {
        return nitContraparte;
    }

    public void setNitContraparte(String nitContraparte) {
        this.nitContraparte = nitContraparte;
    }

    public String getRazonSocialContraparte() {
        return razonSocialContraparte;
    }

    public void setRazonSocialContraparte(String razonSocialContraparte) {
        this.razonSocialContraparte = razonSocialContraparte;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getImpuestos() {
        return impuestos;
    }

    public void setImpuestos(BigDecimal impuestos) {
        this.impuestos = impuestos;
    }

    public BigDecimal getRetenciones() {
        return retenciones;
    }

    public void setRetenciones(BigDecimal retenciones) {
        this.retenciones = retenciones;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstadoRadian() {
        return estadoRadian;
    }

    public void setEstadoRadian(String estadoRadian) {
        this.estadoRadian = estadoRadian;
    }

    public String getEstadoCausacion() {
        return estadoCausacion;
    }

    public void setEstadoCausacion(String estadoCausacion) {
        this.estadoCausacion = estadoCausacion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DocumentoRadianLineaJpa> getLineas() {
        return lineas;
    }

    public List<DocumentoRadianImpuestoJpa> getImpuestosDetalle() {
        return impuestosDetalle;
    }
}
