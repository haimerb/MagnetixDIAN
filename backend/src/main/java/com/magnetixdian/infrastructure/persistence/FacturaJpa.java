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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "factura")
public class FacturaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaJpa empresa;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    private String tipoDocumento = "01";

    @Column(length = 10)
    private String prefijo;

    @Column(nullable = false)
    private long numero;

    @Column(name = "numero_completo", nullable = false, length = 30)
    private String numeroCompleto;

    @Column(length = 200)
    private String cufe;

    @Column(length = 200)
    private String cude;

    @Column(nullable = false)
    private short ambiente = 2;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "hora_emision", nullable = false)
    private LocalTime horaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(nullable = false, length = 3)
    private String moneda = "COP";

    @Column(name = "forma_pago", nullable = false, length = 10)
    private String formaPago = "1";

    @Column(name = "medio_pago", nullable = false, length = 10)
    private String medioPago = "10";

    @Column(name = "tipo_operacion", nullable = false, length = 10)
    private String tipoOperacion = "10";

    @Column(name = "adquirente_tipo_doc", nullable = false, length = 10)
    private String adquirenteTipoDoc = "13";

    @Column(name = "adquirente_num_doc", nullable = false, length = 20)
    private String adquirenteNumDoc;

    @Column(name = "adquirente_dv")
    private Integer adquirenteDv;

    @Column(name = "adquirente_nombre", nullable = false, length = 300)
    private String adquirenteNombre;

    @Column(name = "adquirente_email", length = 200)
    private String adquirenteEmail;

    @Column(name = "adquirente_direccion", length = 200)
    private String adquirenteDireccion;

    @Column(name = "adquirente_municipio", length = 10)
    private String adquirenteMunicipio;

    @Column(name = "adquirente_regimen", length = 20)
    private String adquirenteRegimen = "ORDINARIO";

    @Column(name = "adquirente_responsabilidad", length = 10)
    private String adquirenteResponsabilidad = "R-99-PN";

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total_impuestos", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalImpuestos = BigDecimal.ZERO;

    @Column(name = "total_retenciones", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRetenciones = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "total_pagar", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPagar = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(nullable = false, length = 20)
    private String estado = "BORRADOR";

    @Column(name = "xml_generado", columnDefinition = "text")
    private String xmlGenerado;

    @Column(name = "xml_firmado", columnDefinition = "text")
    private String xmlFirmado;

    @Column(name = "pdf_path", length = 300)
    private String pdfPath;

    @Column(name = "qr_url", length = 600)
    private String qrUrl;

    @Column(name = "respuesta_dian", columnDefinition = "text")
    private String respuestaDian;

    @Column(name = "cufe_referencia", length = 200)
    private String cufeReferencia;

    @Column(name = "numero_referencia", length = 30)
    private String numeroReferencia;

    @Column(name = "fecha_validacion")
    private OffsetDateTime fechaValidacion;

    @Column(name = "usuario_creador")
    private Long usuarioCreador;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numeroLinea ASC")
    private List<FacturaLineaJpa> lineas = new ArrayList<>();

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FacturaImpuestoJpa> impuestos = new ArrayList<>();

    public void addLinea(FacturaLineaJpa linea) {
        linea.setFactura(this);
        this.lineas.add(linea);
    }

    public void addImpuesto(FacturaImpuestoJpa impuesto) {
        impuesto.setFactura(this);
        this.impuestos.add(impuesto);
    }

    public Long getId() {
        return id;
    }

    public EmpresaJpa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaJpa empresa) {
        this.empresa = empresa;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public void setPrefijo(String prefijo) {
        this.prefijo = prefijo;
    }

    public long getNumero() {
        return numero;
    }

    public void setNumero(long numero) {
        this.numero = numero;
    }

    public String getNumeroCompleto() {
        return numeroCompleto;
    }

    public void setNumeroCompleto(String numeroCompleto) {
        this.numeroCompleto = numeroCompleto;
    }

    public String getCufe() {
        return cufe;
    }

    public void setCufe(String cufe) {
        this.cufe = cufe;
    }

    public String getCude() {
        return cude;
    }

    public void setCude(String cude) {
        this.cude = cude;
    }

    public short getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(short ambiente) {
        this.ambiente = ambiente;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalTime getHoraEmision() {
        return horaEmision;
    }

    public void setHoraEmision(LocalTime horaEmision) {
        this.horaEmision = horaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(String formaPago) {
        this.formaPago = formaPago;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public String getTipoOperacion() {
        return tipoOperacion;
    }

    public void setTipoOperacion(String tipoOperacion) {
        this.tipoOperacion = tipoOperacion;
    }

    public String getAdquirenteTipoDoc() {
        return adquirenteTipoDoc;
    }

    public void setAdquirenteTipoDoc(String adquirenteTipoDoc) {
        this.adquirenteTipoDoc = adquirenteTipoDoc;
    }

    public String getAdquirenteNumDoc() {
        return adquirenteNumDoc;
    }

    public void setAdquirenteNumDoc(String adquirenteNumDoc) {
        this.adquirenteNumDoc = adquirenteNumDoc;
    }

    public Integer getAdquirenteDv() {
        return adquirenteDv;
    }

    public void setAdquirenteDv(Integer adquirenteDv) {
        this.adquirenteDv = adquirenteDv;
    }

    public String getAdquirenteNombre() {
        return adquirenteNombre;
    }

    public void setAdquirenteNombre(String adquirenteNombre) {
        this.adquirenteNombre = adquirenteNombre;
    }

    public String getAdquirenteEmail() {
        return adquirenteEmail;
    }

    public void setAdquirenteEmail(String adquirenteEmail) {
        this.adquirenteEmail = adquirenteEmail;
    }

    public String getAdquirenteDireccion() {
        return adquirenteDireccion;
    }

    public void setAdquirenteDireccion(String adquirenteDireccion) {
        this.adquirenteDireccion = adquirenteDireccion;
    }

    public String getAdquirenteMunicipio() {
        return adquirenteMunicipio;
    }

    public void setAdquirenteMunicipio(String adquirenteMunicipio) {
        this.adquirenteMunicipio = adquirenteMunicipio;
    }

    public String getAdquirenteRegimen() {
        return adquirenteRegimen;
    }

    public void setAdquirenteRegimen(String adquirenteRegimen) {
        this.adquirenteRegimen = adquirenteRegimen;
    }

    public String getAdquirenteResponsabilidad() {
        return adquirenteResponsabilidad;
    }

    public void setAdquirenteResponsabilidad(String adquirenteResponsabilidad) {
        this.adquirenteResponsabilidad = adquirenteResponsabilidad;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTotalImpuestos() {
        return totalImpuestos;
    }

    public void setTotalImpuestos(BigDecimal totalImpuestos) {
        this.totalImpuestos = totalImpuestos;
    }

    public BigDecimal getTotalRetenciones() {
        return totalRetenciones;
    }

    public void setTotalRetenciones(BigDecimal totalRetenciones) {
        this.totalRetenciones = totalRetenciones;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotalPagar() {
        return totalPagar;
    }

    public void setTotalPagar(BigDecimal totalPagar) {
        this.totalPagar = totalPagar;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getXmlGenerado() {
        return xmlGenerado;
    }

    public void setXmlGenerado(String xmlGenerado) {
        this.xmlGenerado = xmlGenerado;
    }

    public String getXmlFirmado() {
        return xmlFirmado;
    }

    public void setXmlFirmado(String xmlFirmado) {
        this.xmlFirmado = xmlFirmado;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public String getQrUrl() {
        return qrUrl;
    }

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    public String getRespuestaDian() {
        return respuestaDian;
    }

    public void setRespuestaDian(String respuestaDian) {
        this.respuestaDian = respuestaDian;
    }

    public String getCufeReferencia() {
        return cufeReferencia;
    }

    public void setCufeReferencia(String cufeReferencia) {
        this.cufeReferencia = cufeReferencia;
    }

    public String getNumeroReferencia() {
        return numeroReferencia;
    }

    public void setNumeroReferencia(String numeroReferencia) {
        this.numeroReferencia = numeroReferencia;
    }

    public OffsetDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(OffsetDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public Long getUsuarioCreador() {
        return usuarioCreador;
    }

    public void setUsuarioCreador(Long usuarioCreador) {
        this.usuarioCreador = usuarioCreador;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<FacturaLineaJpa> getLineas() {
        return lineas;
    }

    public List<FacturaImpuestoJpa> getImpuestos() {
        return impuestos;
    }
}
