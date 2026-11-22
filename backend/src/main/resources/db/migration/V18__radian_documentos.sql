CREATE TABLE documento_radian (
    id                        BIGSERIAL PRIMARY KEY,
    empresa_id                BIGINT       NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    direccion                 VARCHAR(10)  NOT NULL,
    tipo_documento            VARCHAR(2)   NOT NULL,
    cufe_cude                 VARCHAR(200) NOT NULL,
    consecutivo               VARCHAR(50)  NOT NULL,
    nit_contraparte           VARCHAR(20),
    razon_social_contraparte  VARCHAR(300),
    fecha                     DATE         NOT NULL,
    subtotal                  NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuestos                 NUMERIC(18,2) NOT NULL DEFAULT 0,
    retenciones               NUMERIC(18,2) NOT NULL DEFAULT 0,
    total                     NUMERIC(18,2) NOT NULL DEFAULT 0,
    estado_radian             VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    estado_causacion          VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, direccion, cufe_cude)
);

CREATE INDEX idx_documento_radian_empresa ON documento_radian(empresa_id);
CREATE INDEX idx_documento_radian_cufe ON documento_radian(cufe_cude);
CREATE INDEX idx_documento_radian_filtros ON documento_radian(empresa_id, direccion, tipo_documento, estado_causacion);

CREATE TABLE documento_radian_linea (
    id                    BIGSERIAL PRIMARY KEY,
    documento_radian_id   BIGINT        NOT NULL REFERENCES documento_radian(id) ON DELETE CASCADE,
    numero_linea          INT           NOT NULL,
    codigo_producto       VARCHAR(50),
    descripcion           VARCHAR(500)  NOT NULL,
    cantidad              NUMERIC(18,6) NOT NULL DEFAULT 1,
    valor_unitario        NUMERIC(18,2) NOT NULL DEFAULT 0,
    subtotal              NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuestos             NUMERIC(18,2) NOT NULL DEFAULT 0,
    total                 NUMERIC(18,2) NOT NULL DEFAULT 0,
    UNIQUE (documento_radian_id, numero_linea)
);

CREATE INDEX idx_documento_radian_linea ON documento_radian_linea(documento_radian_id);

CREATE TABLE documento_radian_impuesto (
    id                    BIGSERIAL PRIMARY KEY,
    documento_radian_id   BIGINT        NOT NULL REFERENCES documento_radian(id) ON DELETE CASCADE,
    tipo                  VARCHAR(10)   NOT NULL,
    nombre                VARCHAR(60),
    base                  NUMERIC(18,2) NOT NULL DEFAULT 0,
    porcentaje            NUMERIC(5,2)  NOT NULL DEFAULT 0,
    valor                 NUMERIC(18,2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_documento_radian_impuesto ON documento_radian_impuesto(documento_radian_id);

CREATE TABLE causacion_radian (
    id                    BIGSERIAL PRIMARY KEY,
    documento_radian_id   BIGINT       NOT NULL UNIQUE REFERENCES documento_radian(id) ON DELETE CASCADE,
    empresa_id            BIGINT       NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    fecha_causacion       DATE         NOT NULL,
    total                 NUMERIC(18,2) NOT NULL DEFAULT 0,
    observaciones         VARCHAR(500),
    usuario_id            BIGINT,
    estado                VARCHAR(20)  NOT NULL DEFAULT 'CAUSADA',
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_causacion_radian_empresa ON causacion_radian(empresa_id);
