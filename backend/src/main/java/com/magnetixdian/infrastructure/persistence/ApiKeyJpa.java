package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
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
@Table(name = "api_key")
public class ApiKeyJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaJpa empresa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioJpa usuario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 16)
    private String prefijo;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "ultimo_uso")
    private OffsetDateTime ultimoUso;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "revocado_at")
    private OffsetDateTime revocadoAt;

    public Long getId() {
        return id;
    }

    public EmpresaJpa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaJpa empresa) {
        this.empresa = empresa;
    }

    public UsuarioJpa getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioJpa usuario) {
        this.usuario = usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public void setPrefijo(String prefijo) {
        this.prefijo = prefijo;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public OffsetDateTime getUltimoUso() {
        return ultimoUso;
    }

    public void setUltimoUso(OffsetDateTime ultimoUso) {
        this.ultimoUso = ultimoUso;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRevocadoAt() {
        return revocadoAt;
    }

    public void setRevocadoAt(OffsetDateTime revocadoAt) {
        this.revocadoAt = revocadoAt;
    }
}
