package com.magnetixdian.infrastructure.integracion.erp.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "erp_integracion_config")
public class ErpIntegracionConfigJpa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(nullable = false, length = 40)
    private String proveedor;
    @Column(nullable = false, length = 40)
    private String tipo = "ERP";
    @Column(nullable = false)
    private boolean activo = true;
    @Column(name = "credenciales_cifradas", columnDefinition = "TEXT")
    private String credencialesCifradas;
    @Column(columnDefinition = "TEXT")
    private String configuracion;
    @Column(nullable = false, length = 30)
    private String estado = "SIMULADO";
    @Column(name = "ultimo_error", length = 1000)
    private String ultimoError;
    @Column(name = "ultima_sincronizacion")
    private OffsetDateTime ultimaSincronizacion;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    @Column(name = "created_by", length = 120)
    private String createdBy;
    @Column(name = "updated_by", length = 120)
    private String updatedBy;
    @PreUpdate
    void preUpdate() { updatedAt = OffsetDateTime.now(); }
    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public String getCredencialesCifradas() { return credencialesCifradas; }
    public void setCredencialesCifradas(String credencialesCifradas) { this.credencialesCifradas = credencialesCifradas; }
    public String getConfiguracion() { return configuracion; }
    public void setConfiguracion(String configuracion) { this.configuracion = configuracion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getUltimoError() { return ultimoError; }
    public void setUltimoError(String ultimoError) { this.ultimoError = ultimoError; }
    public OffsetDateTime getUltimaSincronizacion() { return ultimaSincronizacion; }
    public void setUltimaSincronizacion(OffsetDateTime ultimaSincronizacion) { this.ultimaSincronizacion = ultimaSincronizacion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
