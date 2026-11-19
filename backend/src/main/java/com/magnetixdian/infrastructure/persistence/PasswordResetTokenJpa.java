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
 * Token de un solo uso para restablecer contraseña o aceptar una invitación.
 * Se almacena únicamente el hash SHA-256 del token.
 */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetTokenJpa {

    public static final String TIPO_RESET = "RESET";
    public static final String TIPO_INVITACION = "INVITACION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioJpa usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "expira_at", nullable = false)
    private OffsetDateTime expiraAt;

    @Column(name = "usado_at")
    private OffsetDateTime usadoAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public PasswordResetTokenJpa() {
        this.createdAt = OffsetDateTime.now();
    }

    public PasswordResetTokenJpa(UsuarioJpa usuario, String tokenHash, String tipo, OffsetDateTime expiraAt) {
        this();
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.tipo = tipo;
        this.expiraAt = expiraAt;
    }

    public Long getId() {
        return id;
    }

    public UsuarioJpa getUsuario() {
        return usuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getTipo() {
        return tipo;
    }

    public OffsetDateTime getExpiraAt() {
        return expiraAt;
    }

    public OffsetDateTime getUsadoAt() {
        return usadoAt;
    }

    public void setUsadoAt(OffsetDateTime usadoAt) {
        this.usadoAt = usadoAt;
    }

    public boolean vigente() {
        return usadoAt == null && expiraAt.isAfter(OffsetDateTime.now());
    }
}
