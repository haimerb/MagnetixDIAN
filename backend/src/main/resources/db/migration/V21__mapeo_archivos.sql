CREATE TABLE mapeo_archivo (
    id                BIGSERIAL PRIMARY KEY,
    empresa_id        BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    formato_detectado VARCHAR(80) NOT NULL,
    cabecera_origen   VARCHAR(300) NOT NULL,
    campo             VARCHAR(80) NOT NULL,
    indice            INTEGER NOT NULL,
    confianza         DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_mapeo_archivo_clave UNIQUE (empresa_id, formato_detectado, cabecera_origen)
);

CREATE TABLE importacion_staging (
    id           BIGSERIAL PRIMARY KEY,
    empresa_id   BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    archivo_id   VARCHAR(80) NOT NULL,
    tipo         VARCHAR(40) NOT NULL,
    external_id  VARCHAR(300) NOT NULL,
    fila         INTEGER NOT NULL DEFAULT 0,
    estado       VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    error        VARCHAR(1000),
    payload      TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_importacion_staging_idempotencia UNIQUE (empresa_id, archivo_id, tipo, external_id)
);

CREATE INDEX idx_mapeo_archivo_empresa ON mapeo_archivo(empresa_id, formato_detectado);
CREATE INDEX idx_importacion_staging_archivo ON importacion_staging(empresa_id, archivo_id, tipo);
