-- =============================================================================
-- V12: API keys por empresa (Fase 3 — PR B)
--   La clave completa solo se muestra al crearla; se persiste su hash SHA-256.
--   El prefijo permite localizar la clave sin exponer el secreto.
-- =============================================================================

CREATE TABLE api_key (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    nombre      VARCHAR(80) NOT NULL,
    prefijo     VARCHAR(16) NOT NULL,
    hash        VARCHAR(64) NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_uso  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    revocado_at TIMESTAMPTZ
);

CREATE INDEX idx_api_key_prefijo ON api_key(prefijo);
CREATE INDEX idx_api_key_empresa ON api_key(empresa_id);
