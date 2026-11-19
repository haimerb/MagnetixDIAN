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

/**
 * Refresh token persistido por JTI para permitir rotación y revocación.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshTokenJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioJpa usuario;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "expira_at", nullable = false)
    private OffsetDateTime expiraAt;

    @Column(nullable = false)
    private Boolean revocado = Boolean.FALSE;

    @Column(name = "revocado_at")
    private OffsetDateTime revocadoAt;

    @Column(name = "reemplazado_por", length = 64)
    private String reemplazadoPor;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public RefreshTokenJpa() {
        this.createdAt = OffsetDateTime.now();
    }

    public RefreshTokenJpa(UsuarioJpa usuario, String jti, OffsetDateTime expiraAt) {
        this();
        this.usuario = usuario;
        this.jti = jti;
        this.expiraAt = expiraAt;
    }

    public Long getId() {
        return id;
    }

    public UsuarioJpa getUsuario() {
        return usuario;
    }

    public String getJti() {
        return jti;
    }

    public OffsetDateTime getExpiraAt() {
        return expiraAt;
    }

    public Boolean getRevocado() {
        return revocado;
    }

    public String getReemplazadoPor() {
        return reemplazadoPor;
    }

    public boolean activo() {
        return !Boolean.TRUE.equals(revocado) && expiraAt.isAfter(OffsetDateTime.now());
    }

    public void revocar(String reemplazadoPor) {
        this.revocado = Boolean.TRUE;
        this.revocadoAt = OffsetDateTime.now();
        this.reemplazadoPor = reemplazadoPor;
    }
}
