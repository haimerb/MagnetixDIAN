package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
public class UsuarioJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    @Column(name = "password_actualizado_at")
    private OffsetDateTime passwordActualizadoAt;

    @Column(name = "invitacion_pendiente", nullable = false)
    private Boolean invitacionPendiente = Boolean.FALSE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_role",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleJpa> roles = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id")
    private EmpresaJpa empresa;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UsuarioJpa() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(Integer intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public OffsetDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(OffsetDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public OffsetDateTime getPasswordActualizadoAt() {
        return passwordActualizadoAt;
    }

    public void setPasswordActualizadoAt(OffsetDateTime passwordActualizadoAt) {
        this.passwordActualizadoAt = passwordActualizadoAt;
    }

    public Boolean getInvitacionPendiente() {
        return invitacionPendiente;
    }

    public void setInvitacionPendiente(Boolean invitacionPendiente) {
        this.invitacionPendiente = invitacionPendiente;
    }

    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(OffsetDateTime.now());
    }

    public Set<RoleJpa> getRoles() {
        return roles;
    }

    public EmpresaJpa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaJpa empresa) {
        this.empresa = empresa;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}