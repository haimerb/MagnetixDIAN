-- =============================================================================
-- V13: Pagos de suscripción (Fase 3 — PR C)
--   Registra la intención de pago por pasarela; el webhook la confirma y
--   activa el plan correspondiente.
-- =============================================================================

CREATE TABLE pago (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    plan_codigo   VARCHAR(30) NOT NULL,
    proveedor     VARCHAR(20) NOT NULL,
    referencia    VARCHAR(120) NOT NULL UNIQUE,
    monto         NUMERIC(12,2) NOT NULL,
    moneda        VARCHAR(3) NOT NULL DEFAULT 'COP',
    estado        VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    url_checkout  VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pago_empresa ON pago(empresa_id);
