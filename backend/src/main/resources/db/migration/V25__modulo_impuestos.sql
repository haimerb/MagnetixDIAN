CREATE TABLE impuesto_tarifa (
    id               BIGSERIAL PRIMARY KEY,
    impuesto_codigo  VARCHAR(20)  NOT NULL,
    nombre           VARCHAR(100) NOT NULL,
    anio             INT          NOT NULL,
    concepto_codigo  VARCHAR(10),
    porcentaje       NUMERIC(7,4) NOT NULL,
    base_min_uvt     NUMERIC(12,2) NOT NULL DEFAULT 0,
    base_max_uvt     NUMERIC(12,2),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (porcentaje >= 0),
    CHECK (base_min_uvt >= 0),
    CHECK (base_max_uvt IS NULL OR base_max_uvt >= base_min_uvt)
);

CREATE UNIQUE INDEX uq_impuesto_tarifa_vigencia
    ON impuesto_tarifa (impuesto_codigo, anio, COALESCE(concepto_codigo, ''));
CREATE INDEX idx_impuesto_tarifa_anio ON impuesto_tarifa(anio);

INSERT INTO impuesto_tarifa (impuesto_codigo, nombre, anio, concepto_codigo, porcentaje, base_min_uvt)
SELECT v.codigo, v.nombre, y.anio, v.concepto, v.porcentaje, v.min_uvt
FROM (VALUES
    ('01', 'IVA', NULL, 0.1900, 0),
    ('05', 'Retención en la fuente - servicios', '2004', 0.0040, 3),
    ('05', 'Retención en la fuente - honorarios', '2005', 0.1000, 3),
    ('05', 'Retención en la fuente - comisiones', '2007', 0.0600, 3),
    ('05', 'Retención en la fuente - arrendamiento', '2008', 0.0200, 3),
    ('06', 'Retención de IVA', NULL, 0.0600, 0),
    ('07', 'Retención ICA', NULL, 0.0097, 0)
) AS v(codigo, nombre, concepto, porcentaje, min_uvt)
CROSS JOIN (
    SELECT DISTINCT anio
    FROM (VALUES (2025), (EXTRACT(YEAR FROM CURRENT_DATE)::INT)) AS a(anio)
) y;

CREATE TABLE responsabilidad_fiscal (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT       NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    tercero_id       BIGINT       REFERENCES tercero_contable(id) ON DELETE CASCADE,
    impuesto_codigo  VARCHAR(20)  NOT NULL,
    codigo           VARCHAR(20)  NOT NULL,
    desde            DATE         NOT NULL,
    hasta            DATE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (hasta IS NULL OR hasta >= desde)
);

CREATE UNIQUE INDEX uq_responsabilidad_fiscal
    ON responsabilidad_fiscal (empresa_id, COALESCE(tercero_id, 0), impuesto_codigo, codigo, desde);
CREATE INDEX idx_responsabilidad_fiscal_empresa ON responsabilidad_fiscal(empresa_id);

INSERT INTO responsabilidad_fiscal (empresa_id, impuesto_codigo, codigo, desde)
SELECT e.id, i.codigo, 'O-13', DATE '2025-01-01'
FROM empresa e
CROSS JOIN (VALUES ('01'), ('05'), ('06'), ('07')) AS i(codigo)
WHERE e.nit = '900123456'
  AND NOT EXISTS (
      SELECT 1 FROM responsabilidad_fiscal r
      WHERE r.empresa_id = e.id AND r.impuesto_codigo = i.codigo
  );

CREATE TABLE impuesto_causacion (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT        NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    impuesto_codigo  VARCHAR(20)   NOT NULL,
    origen           VARCHAR(30)   NOT NULL,
    origen_id        BIGINT        NOT NULL,
    tercero_id       BIGINT        REFERENCES tercero_contable(id),
    periodo          VARCHAR(7)    NOT NULL,
    fecha            DATE          NOT NULL,
    concepto_codigo  VARCHAR(10),
    base_imponible   NUMERIC(18,2) NOT NULL,
    tarifa           NUMERIC(7,4)  NOT NULL,
    valor            NUMERIC(18,2) NOT NULL,
    estado           VARCHAR(15)   NOT NULL DEFAULT 'CAUSADO',
    observaciones    VARCHAR(300),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CHECK (estado IN ('CAUSADO', 'PAGADO', 'ANULADO'))
);

CREATE UNIQUE INDEX uq_impuesto_causacion_origen
    ON impuesto_causacion (empresa_id, origen, origen_id, impuesto_codigo);
CREATE INDEX idx_impuesto_causacion_empresa_periodo ON impuesto_causacion(empresa_id, periodo);
CREATE INDEX idx_impuesto_causacion_tercero ON impuesto_causacion(tercero_id);

CREATE TABLE impuesto_pago (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT        NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    causacion_id  BIGINT        NOT NULL REFERENCES impuesto_causacion(id) ON DELETE CASCADE,
    fecha         DATE          NOT NULL,
    valor         NUMERIC(18,2) NOT NULL,
    comprobante   VARCHAR(50),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CHECK (valor > 0)
);

CREATE INDEX idx_impuesto_pago_causacion ON impuesto_pago(causacion_id);

CREATE TABLE certificado_impuesto (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT        NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    tercero_id       BIGINT        NOT NULL REFERENCES tercero_contable(id),
    impuesto_codigo  VARCHAR(20)   NOT NULL,
    anio             INT           NOT NULL,
    numero           VARCHAR(40)   NOT NULL,
    valor            NUMERIC(18,2) NOT NULL,
    fecha_emision    DATE          NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_certificado_impuesto
    ON certificado_impuesto (empresa_id, tercero_id, impuesto_codigo, anio, numero);
CREATE INDEX idx_certificado_impuesto_empresa_anio ON certificado_impuesto(empresa_id, anio);

CREATE TABLE declaracion_impuesto (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT        NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    impuesto_codigo  VARCHAR(20)   NOT NULL,
    anio_gravable    INT           NOT NULL,
    periodo          VARCHAR(10)   NOT NULL,
    fecha_desde      DATE          NOT NULL,
    fecha_hasta      DATE          NOT NULL,
    fecha_limite     DATE,
    estado           VARCHAR(15)   NOT NULL DEFAULT 'BORRADOR',
    total_causado    NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_pagado     NUMERIC(18,2) NOT NULL DEFAULT 0,
    saldo            NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CHECK (estado IN ('BORRADOR', 'PRESENTADA')),
    CHECK (fecha_hasta >= fecha_desde)
);

CREATE UNIQUE INDEX uq_declaracion_impuesto
    ON declaracion_impuesto (empresa_id, impuesto_codigo, anio_gravable, periodo);
CREATE INDEX idx_declaracion_impuesto_empresa_anio ON declaracion_impuesto(empresa_id, anio_gravable);

CREATE TABLE calendario_tributario (
    id               BIGSERIAL PRIMARY KEY,
    impuesto_codigo  VARCHAR(20) NOT NULL,
    anio_gravable    INT         NOT NULL,
    periodicidad     VARCHAR(15) NOT NULL,
    periodo          VARCHAR(10) NOT NULL,
    fecha_limite     DATE        NOT NULL,
    UNIQUE (impuesto_codigo, anio_gravable, periodicidad, periodo)
);

INSERT INTO calendario_tributario (impuesto_codigo, anio_gravable, periodicidad, periodo, fecha_limite)
SELECT i.codigo,
       y.anio,
       'MENSUAL',
       y.anio::text || '-' || lpad(m.mes::text, 2, '0'),
       (make_date(y.anio, m.mes, 1) + INTERVAL '1 month 19 days')::date
FROM (VALUES ('01'), ('05'), ('06'), ('07')) AS i(codigo)
CROSS JOIN (
    SELECT DISTINCT anio
    FROM (VALUES (2025), (EXTRACT(YEAR FROM CURRENT_DATE)::INT)) AS a(anio)
) y
CROSS JOIN generate_series(1, 12) AS m(mes)
WHERE NOT EXISTS (
    SELECT 1 FROM calendario_tributario c
    WHERE c.impuesto_codigo = i.codigo
      AND c.anio_gravable = y.anio
      AND c.periodicidad = 'MENSUAL'
      AND c.periodo = y.anio::text || '-' || lpad(m.mes::text, 2, '0')
);
