ALTER TABLE empresa
    ADD COLUMN IF NOT EXISTS estado_tenant VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS plan_codigo VARCHAR(30),
    ADD COLUMN IF NOT EXISTS modo_operacion_dian VARCHAR(30) NOT NULL DEFAULT 'ADQUIRIDO_CLIENTE',
    ADD COLUMN IF NOT EXISTS owner_usuario_id BIGINT,
    ADD COLUMN IF NOT EXISTS email_verificado_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS tenant_activado_at TIMESTAMPTZ;

UPDATE empresa SET estado_tenant = 'ACTIVO' WHERE estado_tenant IS NULL;

ALTER TABLE empresa
    ADD CONSTRAINT chk_empresa_estado_tenant
    CHECK (estado_tenant IN ('PENDIENTE_VERIFICACION', 'ACTIVO', 'SUSPENDIDO', 'CANCELADO'));

CREATE INDEX IF NOT EXISTS idx_empresa_estado_tenant ON empresa(estado_tenant);

INSERT INTO role (code, description) VALUES
    ('ROLE_OWNER', 'Propietario del tenant'),
    ('ROLE_VENDEDOR', 'Vendedor del tenant'),
    ('ROLE_LECTOR', 'Lectura del tenant')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS onboarding_pago (
    event_id     VARCHAR(120) NOT NULL,
    empresa_id   BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    plan_codigo  VARCHAR(30) NOT NULL,
    referencia   VARCHAR(120),
    estado       VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_EMAIL',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    activado_at  TIMESTAMPTZ,
    PRIMARY KEY (event_id, empresa_id)
);

CREATE INDEX IF NOT EXISTS idx_onboarding_pago_empresa_estado
    ON onboarding_pago(empresa_id, estado);

CREATE TABLE IF NOT EXISTS invitacion_tenant (
    token_hash  VARCHAR(64) PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    rol         VARCHAR(30) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_invitacion_tenant_empresa ON invitacion_tenant(empresa_id);
