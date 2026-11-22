package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Registro inmutable de acciones sensibles (login, cambios de clave, emisión, etc.).
 */
@Entity
@Table(name = "evento_auditoria")
public class EventoAuditoriaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(length = 80)
    private String usuario;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(length = 60)
    private String entidad;

    @Column(name = "entidad_id", length = 60)
    private String entidadId;

    @Column(length = 1000)
    private String detalle;

    @Column(length = 64)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public EventoAuditoriaJpa() {
        this.createdAt = OffsetDateTime.now();
    }

    public EventoAuditoriaJpa(String usuario, String accion, String entidad, String entidadId,
                              String detalle, String ip) {
        this(null, usuario, accion, entidad, entidadId, detalle, ip);
    }

    public EventoAuditoriaJpa(Long empresaId, String usuario, String accion, String entidad, String entidadId,
                              String detalle, String ip) {
        this();
        this.empresaId = empresaId;
        this.usuario = usuario;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
        this.ip = ip;
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getUsuario() {
        return usuario;
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

    public String getIp() {
        return ip;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
