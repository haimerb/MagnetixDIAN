-- =============================================================================
-- V9: Endurecimiento de autenticación + auditoría de eventos
--   - Tokens de un solo uso para recuperación de contraseña e invitaciones.
--   - Refresh tokens persistidos (rotación y revocación).
--   - Bloqueo temporal por intentos fallidos.
--   - Bitácora de auditoría para acciones sensibles.
-- =============================================================================

ALTER TABLE usuario ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN bloqueado_hasta TIMESTAMPTZ;
ALTER TABLE usuario ADD COLUMN password_actualizado_at TIMESTAMPTZ;
ALTER TABLE usuario ADD COLUMN invitacion_pendiente BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE password_reset_token (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    tipo       VARCHAR(20) NOT NULL DEFAULT 'RESET', -- RESET | INVITACION
    expira_at  TIMESTAMPTZ NOT NULL,
    usado_at   TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_usuario ON password_reset_token(usuario_id);

CREATE TABLE refresh_token (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    jti            VARCHAR(64) NOT NULL UNIQUE,
    expira_at      TIMESTAMPTZ NOT NULL,
    revocado       BOOLEAN NOT NULL DEFAULT FALSE,
    revocado_at    TIMESTAMPTZ,
    reemplazado_por VARCHAR(64),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_usuario ON refresh_token(usuario_id);

CREATE TABLE evento_auditoria (
    id         BIGSERIAL PRIMARY KEY,
    usuario    VARCHAR(80),
    accion     VARCHAR(60) NOT NULL,
    entidad    VARCHAR(60),
    entidad_id VARCHAR(60),
    detalle    VARCHAR(1000),
    ip         VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auditoria_created ON evento_auditoria(created_at DESC);
CREATE INDEX idx_auditoria_accion ON evento_auditoria(accion);
CREATE INDEX idx_auditoria_usuario ON evento_auditoria(usuario);
