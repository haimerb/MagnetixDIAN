-- =============================================================================
-- V10: Cola de envíos electrónicos con reintentos
--   - Un registro por factura con el estado del último envío al canal.
--   - Backoff exponencial para reintentos automáticos.
-- =============================================================================

CREATE TABLE envio_factura (
    id              BIGSERIAL PRIMARY KEY,
    factura_id      BIGINT NOT NULL UNIQUE REFERENCES factura(id) ON DELETE CASCADE,
    cufe            VARCHAR(100),
    canal           VARCHAR(10) NOT NULL DEFAULT 'SIMULADO',
    estado          VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos        INT NOT NULL DEFAULT 0,
    max_intentos    INT NOT NULL DEFAULT 5,
    proximo_intento TIMESTAMPTZ NOT NULL DEFAULT now(),
    ultimo_error    VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_envio_factura_pendientes ON envio_factura(estado, proximo_intento);
CREATE INDEX idx_envio_factura_actualizado ON envio_factura(updated_at DESC);
