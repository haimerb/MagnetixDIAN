CREATE TABLE cuenta_contable (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    codigo      VARCHAR(10)  NOT NULL,
    nombre      VARCHAR(150) NOT NULL,
    tipo        VARCHAR(20)  NOT NULL,
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_cuenta_contable_empresa_codigo ON cuenta_contable (COALESCE(empresa_id, 0), codigo);

CREATE INDEX idx_cuenta_contable_empresa ON cuenta_contable(empresa_id);

INSERT INTO cuenta_contable (empresa_id, codigo, nombre, tipo)
SELECT NULL, v.codigo, v.nombre, v.tipo
FROM (VALUES
    ('130505', 'Clientes', 'ACTIVO'),
    ('220505', 'Proveedores', 'PASIVO'),
    ('413505', 'Ventas', 'INGRESO'),
    ('413515', 'Devoluciones ventas', 'INGRESO'),
    ('519505', 'Compras', 'EGRESO'),
    ('529515', 'Devoluciones compras', 'EGRESO'),
    ('240805', 'IVA generado', 'PASIVO'),
    ('135515', 'IVA descontable', 'ACTIVO'),
    ('135520', 'Retenciones', 'ACTIVO'),
    ('236505', 'Retención en la fuente', 'PASIVO'),
    ('110505', 'Caja', 'ACTIVO'),
    ('399999', 'Ajustes', 'OTRO')
) AS v(codigo, nombre, tipo)
WHERE NOT EXISTS (
    SELECT 1 FROM cuenta_contable c WHERE c.empresa_id IS NULL AND c.codigo = v.codigo
);

CREATE TABLE asiento_contable (
    id                 BIGSERIAL PRIMARY KEY,
    empresa_id         BIGINT        NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    numero             VARCHAR(30)   NOT NULL,
    fecha              DATE          NOT NULL,
    descripcion        VARCHAR(300),
    documento_tipo     VARCHAR(30)   NOT NULL,
    documento_id       BIGINT        NOT NULL,
    tipo_evento        VARCHAR(30)   NOT NULL,
    estado             VARCHAR(15)   NOT NULL DEFAULT 'CONTABILIZADO',
    total_debito       NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_credito      NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_total        NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_impuestos    NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_retenciones  NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, documento_tipo, documento_id, tipo_evento)
);

CREATE INDEX idx_asiento_contable_empresa ON asiento_contable(empresa_id);
CREATE INDEX idx_asiento_contable_documento ON asiento_contable(documento_tipo, documento_id);

CREATE TABLE asiento_contable_linea (
    id                    BIGSERIAL PRIMARY KEY,
    asiento_contable_id   BIGINT        NOT NULL REFERENCES asiento_contable(id) ON DELETE CASCADE,
    cuenta_contable_id    BIGINT        NOT NULL REFERENCES cuenta_contable(id),
    numero_linea          INT           NOT NULL,
    descripcion           VARCHAR(300),
    debito                NUMERIC(18,2) NOT NULL DEFAULT 0,
    credito               NUMERIC(18,2) NOT NULL DEFAULT 0,
    UNIQUE (asiento_contable_id, numero_linea)
);

CREATE INDEX idx_asiento_linea ON asiento_contable_linea(asiento_contable_id);

CREATE TABLE documento_asiento (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT       NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    documento_radian_id   BIGINT       NOT NULL REFERENCES documento_radian(id) ON DELETE CASCADE,
    asiento_contable_id   BIGINT       NOT NULL REFERENCES asiento_contable(id) ON DELETE CASCADE,
    tipo_evento           VARCHAR(30)  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (documento_radian_id, asiento_contable_id, tipo_evento)
);

CREATE INDEX idx_documento_asiento_empresa ON documento_asiento(empresa_id);

CREATE TABLE conciliacion (
    id                     BIGSERIAL PRIMARY KEY,
    empresa_id             BIGINT       NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    fecha_desde            DATE,
    fecha_hasta            DATE,
    total_documentos       INT          NOT NULL DEFAULT 0,
    coincidencias          INT          NOT NULL DEFAULT 0,
    diferencias            INT          NOT NULL DEFAULT 0,
    documentos_sin_asiento INT          NOT NULL DEFAULT 0,
    valor_diferencia       NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_conciliacion_empresa ON conciliacion(empresa_id);

CREATE TABLE conciliacion_linea (
    id                    BIGSERIAL PRIMARY KEY,
    conciliacion_id       BIGINT        NOT NULL REFERENCES conciliacion(id) ON DELETE CASCADE,
    documento_radian_id   BIGINT        NOT NULL,
    consecutivo           VARCHAR(50),
    asiento_contable_id   BIGINT,
    resultado             VARCHAR(15)   NOT NULL,
    severidad             VARCHAR(15)   NOT NULL,
    campo                 VARCHAR(30)   NOT NULL,
    valor_documento       NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_asiento         NUMERIC(18,2) NOT NULL DEFAULT 0,
    diferencia            NUMERIC(18,2) NOT NULL DEFAULT 0,
    mensaje               VARCHAR(500)
);

CREATE INDEX idx_conciliacion_linea ON conciliacion_linea(conciliacion_id);
