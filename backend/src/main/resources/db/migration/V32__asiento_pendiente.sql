CREATE TABLE IF NOT EXISTS asiento_pendiente (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    origen VARCHAR(40) NOT NULL,
    origen_id BIGINT NOT NULL,
    tipo_evento VARCHAR(20) NOT NULL,
    fecha DATE NOT NULL,
    payload TEXT NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
    intentos INTEGER NOT NULL DEFAULT 0,
    ultimo_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_asiento_pendiente_estado
    ON asiento_pendiente (estado, created_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_asiento_pendiente_origen
    ON asiento_pendiente (empresa_id, origen, origen_id, tipo_evento)
    WHERE estado IN ('PENDIENTE', 'PROCESADO', 'FALLIDO');
