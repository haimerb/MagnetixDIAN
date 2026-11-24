CREATE TABLE billing_checkout (
    id BIGSERIAL PRIMARY KEY,
    referencia VARCHAR(120) NOT NULL UNIQUE,
    empresa_id BIGINT REFERENCES empresa(id) ON DELETE SET NULL,
    plan_code VARCHAR(30) NOT NULL,
    correo VARCHAR(150),
    proveedor VARCHAR(20) NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    moneda VARCHAR(3) NOT NULL DEFAULT 'COP',
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    checkout_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_billing_checkout_empresa ON billing_checkout(empresa_id);

CREATE TABLE billing_webhook_event (
    id BIGSERIAL PRIMARY KEY,
    proveedor VARCHAR(20) NOT NULL,
    event_id VARCHAR(120) NOT NULL,
    referencia VARCHAR(120),
    estado VARCHAR(20) NOT NULL,
    monto NUMERIC(12,2),
    moneda VARCHAR(3),
    resultado VARCHAR(20) NOT NULL,
    comprobante_numero VARCHAR(30),
    mensaje VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_billing_webhook_event UNIQUE (proveedor, event_id)
);

CREATE SEQUENCE billing_comprobante_numero_seq START 1;

CREATE TABLE billing_comprobante (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(30) NOT NULL UNIQUE,
    referencia VARCHAR(120) NOT NULL,
    empresa_id BIGINT,
    nit VARCHAR(20) NOT NULL,
    plan_code VARCHAR(30) NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    impuestos NUMERIC(12,2) NOT NULL DEFAULT 0,
    moneda VARCHAR(3) NOT NULL DEFAULT 'COP',
    estado VARCHAR(20) NOT NULL DEFAULT 'PAGADO',
    pdf_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_billing_comprobante_referencia ON billing_comprobante(referencia);

CREATE TABLE billing_entitlement (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    plan_code VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    proveedor VARCHAR(20),
    referencia VARCHAR(120),
    fecha_inicio DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_expiracion DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_billing_entitlement_activa ON billing_entitlement(empresa_id) WHERE estado = 'ACTIVA';
