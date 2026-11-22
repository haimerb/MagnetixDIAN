package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "credencial_auditoria")
public class CredencialAuditoriaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "key_id", nullable = false, length = 60)
    private String keyId;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(length = 60)
    private String entidad;

    @Column(name = "entidad_id", length = 60)
    private String entidadId;

    @Column(length = 1000)
    private String detalle;

    @Column(length = 80)
    private String usuario;

    @Column(length = 64)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public CredencialAuditoriaJpa() {
    }

    public CredencialAuditoriaJpa(Long empresaId, String keyId, String accion, String entidad,
                                  String entidadId, String detalle, String usuario, String ip) {
        this.empresaId = empresaId;
        this.keyId = keyId;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
        this.usuario = usuario;
        this.ip = ip;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getAccion() {
        return accion;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getIp() {
        return ip;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
