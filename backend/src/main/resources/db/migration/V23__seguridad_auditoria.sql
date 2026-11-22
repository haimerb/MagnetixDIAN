ALTER TABLE evento_auditoria ADD COLUMN empresa_id BIGINT REFERENCES empresa(id) ON DELETE SET NULL;

CREATE INDEX idx_auditoria_empresa ON evento_auditoria(empresa_id, created_at DESC);

CREATE TABLE credencial_auditoria (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT REFERENCES empresa(id) ON DELETE SET NULL,
    key_id VARCHAR(60) NOT NULL,
    accion VARCHAR(60) NOT NULL,
    entidad VARCHAR(60),
    entidad_id VARCHAR(60),
    detalle VARCHAR(1000),
    usuario VARCHAR(80),
    ip VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_credencial_auditoria_empresa ON credencial_auditoria(empresa_id, created_at DESC);
CREATE INDEX idx_credencial_auditoria_key ON credencial_auditoria(key_id);
