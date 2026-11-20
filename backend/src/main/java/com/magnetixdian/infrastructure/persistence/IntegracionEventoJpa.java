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
@Table(name = "integracion_evento")
public class IntegracionEventoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaJpa empresa;

    @Column(nullable = false, length = 40)
    private String proveedor;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(name = "external_id", nullable = false, length = 200)
    private String externalId;

    @Convert(converter = CifradoAtributoConverter.class)
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(nullable = false)
    private int intentos = 0;

    @Column(name = "proximo_intento")
    private OffsetDateTime proximoIntento;

    @Column(name = "ultimo_error", length = 1000)
    private String ultimoError;

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public void setIntentos(int intentos) {
        this.intentos = intentos;
    }

    public OffsetDateTime getProximoIntento() {
        return proximoIntento;
    }

    public void setProximoIntento(OffsetDateTime proximoIntento) {
        this.proximoIntento = proximoIntento;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public void setUltimoError(String ultimoError) {
        this.ultimoError = ultimoError;
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
