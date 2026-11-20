CREATE TABLE integracion_config (
    id                     BIGSERIAL PRIMARY KEY,
    empresa_id             BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    proveedor              VARCHAR(40) NOT NULL,
    activo                 BOOLEAN NOT NULL DEFAULT TRUE,
    credenciales           TEXT,
    configuracion          TEXT,
    estado                 VARCHAR(20) NOT NULL DEFAULT 'DESCONFIGURADO',
    ultimo_error           VARCHAR(1000),
    ultima_sincronizacion  TIMESTAMPTZ,
    webhook_token_hash     VARCHAR(64),
    webhook_token_prefijo  VARCHAR(12),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_integracion_config_empresa_proveedor UNIQUE (empresa_id, proveedor)
);

CREATE TABLE integracion_evento (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    proveedor        VARCHAR(40) NOT NULL,
    tipo             VARCHAR(80) NOT NULL,
    external_id      VARCHAR(200) NOT NULL,
    payload          TEXT,
    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos         INTEGER NOT NULL DEFAULT 0,
    proximo_intento  TIMESTAMPTZ,
    ultimo_error     VARCHAR(1000),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_integracion_evento_idempotencia UNIQUE (empresa_id, proveedor, external_id, tipo)
);

CREATE INDEX idx_integracion_config_empresa ON integracion_config(empresa_id);
CREATE INDEX idx_integracion_config_webhook ON integracion_config(webhook_token_hash);
CREATE INDEX idx_integracion_evento_empresa ON integracion_evento(empresa_id, created_at DESC);
CREATE INDEX idx_integracion_evento_pendiente ON integracion_evento(estado, proximo_intento);
