package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "empresa_fe_config")
public class EmpresaFeConfigJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private EmpresaJpa empresa;

    @Column(nullable = false)
    private short ambiente = 2;

    @Column(name = "software_id", length = 100)
    private String softwareId;

    @Column(name = "software_pin", length = 100)
    private String softwarePin;

    @Column(name = "software_security_code", length = 200)
    private String softwareSecurityCode;

    @Column(name = "test_set_id", length = 100)
    private String testSetId;

    @Column(name = "resolucion_numero", length = 50)
    private String resolucionNumero;

    @Column(name = "resolucion_fecha")
    private LocalDate resolucionFecha;

    @Column(length = 10)
    private String prefijo;

    @Column(name = "rango_desde")
    private Long rangoDesde;

    @Column(name = "rango_hasta")
    private Long rangoHasta;

    @Column(name = "rango_vigencia_desde")
    private LocalDate rangoVigenciaDesde;

    @Column(name = "rango_vigencia_hasta")
    private LocalDate rangoVigenciaHasta;

    @Column(nullable = false, length = 20)
    private String canal = "PAC";

    @Column(name = "pac_proveedor", length = 40)
    private String pacProveedor;

    @Column(name = "pac_base_url", length = 300)
    private String pacBaseUrl;

    @Column(name = "pac_api_key", length = 300)
    private String pacApiKey;

    @Column(name = "certificado_nombre", length = 200)
    private String certificadoNombre;

    @Column(name = "certificado_password", length = 200)
    private String certificadoPassword;

    @Column(name = "certificado_path", length = 300)
    private String certificadoPath;

    @Column(name = "responsabilidad_fiscal", nullable = false, length = 10)
    private String responsabilidadFiscal = "R-99-PN";

    @Column(name = "codigo_municipio", length = 10)
    private String codigoMunicipio;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public EmpresaJpa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaJpa empresa) {
        this.empresa = empresa;
    }

    public short getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(short ambiente) {
        this.ambiente = ambiente;
    }

    public String getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(String softwareId) {
        this.softwareId = softwareId;
    }

    public String getSoftwarePin() {
        return softwarePin;
    }

    public void setSoftwarePin(String softwarePin) {
        this.softwarePin = softwarePin;
    }

    public String getSoftwareSecurityCode() {
        return softwareSecurityCode;
    }

    public void setSoftwareSecurityCode(String softwareSecurityCode) {
        this.softwareSecurityCode = softwareSecurityCode;
    }

    public String getTestSetId() {
        return testSetId;
    }

    public void setTestSetId(String testSetId) {
        this.testSetId = testSetId;
    }

    public String getResolucionNumero() {
        return resolucionNumero;
    }

    public void setResolucionNumero(String resolucionNumero) {
        this.resolucionNumero = resolucionNumero;
    }

    public LocalDate getResolucionFecha() {
        return resolucionFecha;
    }

    public void setResolucionFecha(LocalDate resolucionFecha) {
        this.resolucionFecha = resolucionFecha;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public void setPrefijo(String prefijo) {
        this.prefijo = prefijo;
    }

    public Long getRangoDesde() {
        return rangoDesde;
    }

    public void setRangoDesde(Long rangoDesde) {
        this.rangoDesde = rangoDesde;
    }

    public Long getRangoHasta() {
        return rangoHasta;
    }

    public void setRangoHasta(Long rangoHasta) {
        this.rangoHasta = rangoHasta;
    }

    public LocalDate getRangoVigenciaDesde() {
        return rangoVigenciaDesde;
    }

    public void setRangoVigenciaDesde(LocalDate rangoVigenciaDesde) {
        this.rangoVigenciaDesde = rangoVigenciaDesde;
    }

    public LocalDate getRangoVigenciaHasta() {
        return rangoVigenciaHasta;
    }

    public void setRangoVigenciaHasta(LocalDate rangoVigenciaHasta) {
        this.rangoVigenciaHasta = rangoVigenciaHasta;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getPacProveedor() {
        return pacProveedor;
    }

    public void setPacProveedor(String pacProveedor) {
        this.pacProveedor = pacProveedor;
    }

    public String getPacBaseUrl() {
        return pacBaseUrl;
    }

    public void setPacBaseUrl(String pacBaseUrl) {
        this.pacBaseUrl = pacBaseUrl;
    }

    public String getPacApiKey() {
        return pacApiKey;
    }

    public void setPacApiKey(String pacApiKey) {
        this.pacApiKey = pacApiKey;
    }

    public String getCertificadoNombre() {
        return certificadoNombre;
    }

    public void setCertificadoNombre(String certificadoNombre) {
        this.certificadoNombre = certificadoNombre;
    }

    public String getCertificadoPassword() {
        return certificadoPassword;
    }

    public void setCertificadoPassword(String certificadoPassword) {
        this.certificadoPassword = certificadoPassword;
    }

    public String getCertificadoPath() {
        return certificadoPath;
    }

    public void setCertificadoPath(String certificadoPath) {
        this.certificadoPath = certificadoPath;
    }

    public String getResponsabilidadFiscal() {
        return responsabilidadFiscal;
    }

    public void setResponsabilidadFiscal(String responsabilidadFiscal) {
        this.responsabilidadFiscal = responsabilidadFiscal;
    }

    public String getCodigoMunicipio() {
        return codigoMunicipio;
    }

    public void setCodigoMunicipio(String codigoMunicipio) {
        this.codigoMunicipio = codigoMunicipio;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
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
}
