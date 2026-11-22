CREATE TABLE migracion_contable (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    proveedor   VARCHAR(40) NOT NULL,
    etapas      TEXT NOT NULL,
    periodo     VARCHAR(20),
    estado      VARCHAR(20) NOT NULL DEFAULT 'EN_CURSO',
    exitosos    INTEGER NOT NULL DEFAULT 0,
    omitidos    INTEGER NOT NULL DEFAULT 0,
    errores     INTEGER NOT NULL DEFAULT 0,
    total       INTEGER NOT NULL DEFAULT 0,
    mensaje     VARCHAR(1000),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE migracion_contable_detalle (
    id            BIGSERIAL PRIMARY KEY,
    migracion_id  BIGINT NOT NULL REFERENCES migracion_contable(id) ON DELETE CASCADE,
    empresa_id    BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    proveedor     VARCHAR(40) NOT NULL,
    tipo          VARCHAR(40) NOT NULL,
    external_id   VARCHAR(300) NOT NULL,
    estado        VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    error         VARCHAR(1000),
    payload       TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_migracion_contable_detalle_idempotencia
        UNIQUE (empresa_id, proveedor, tipo, external_id)
);

CREATE INDEX idx_migracion_contable_empresa ON migracion_contable(empresa_id, created_at DESC);
CREATE INDEX idx_migracion_contable_detalle_migracion ON migracion_contable_detalle(migracion_id);
CREATE INDEX idx_migracion_contable_detalle_empresa ON migracion_contable_detalle(empresa_id, proveedor, tipo);
