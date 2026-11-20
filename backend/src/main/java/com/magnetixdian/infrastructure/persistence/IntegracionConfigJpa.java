package com.magnetixdian.infrastructure.persistence;

import com.magnetixdian.infrastructure.crypto.CifradoAtributoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "integracion_config")
public class IntegracionConfigJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaJpa empresa;

    @Column(nullable = false, length = 40)
    private String proveedor;

    @Column(nullable = false)
    private boolean activo = true;

    @Convert(converter = CifradoAtributoConverter.class)
    @Column(name = "credenciales", columnDefinition = "TEXT")
    private String credenciales;

    @Convert(converter = CifradoAtributoConverter.class)
    @Column(name = "configuracion", columnDefinition = "TEXT")
    private String configuracion;

    @Column(nullable = false, length = 20)
    private String estado = "DESCONFIGURADO";

    @Column(name = "ultimo_error", length = 1000)
    private String ultimoError;

    @Column(name = "ultima_sincronizacion")
    private OffsetDateTime ultimaSincronizacion;

    @Column(name = "webhook_token_hash", length = 64)
    private String webhookTokenHash;

    @Column(name = "webhook_token_prefijo", length = 12)
    private String webhookTokenPrefijo;

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

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getCredenciales() {
        return credenciales;
    }

    public void setCredenciales(String credenciales) {
        this.credenciales = credenciales;
    }

    public String getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(String configuracion) {
        this.configuracion = configuracion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public void setUltimoError(String ultimoError) {
        this.ultimoError = ultimoError;
    }

    public OffsetDateTime getUltimaSincronizacion() {
        return ultimaSincronizacion;
    }

    public void setUltimaSincronizacion(OffsetDateTime ultimaSincronizacion) {
        this.ultimaSincronizacion = ultimaSincronizacion;
    }

    public String getWebhookTokenHash() {
        return webhookTokenHash;
    }

    public void setWebhookTokenHash(String webhookTokenHash) {
        this.webhookTokenHash = webhookTokenHash;
    }

    public String getWebhookTokenPrefijo() {
        return webhookTokenPrefijo;
    }

    public void setWebhookTokenPrefijo(String webhookTokenPrefijo) {
        this.webhookTokenPrefijo = webhookTokenPrefijo;
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
