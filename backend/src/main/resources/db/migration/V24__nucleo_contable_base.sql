CREATE TABLE periodo_contable (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT       NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    anio          INT          NOT NULL,
    mes           INT          NOT NULL CHECK (mes BETWEEN 1 AND 12),
    fecha_desde   DATE         NOT NULL,
    fecha_hasta   DATE         NOT NULL,
    estado        VARCHAR(15)  NOT NULL DEFAULT 'ABIERTO',
    cerrado_en    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, anio, mes),
    CHECK (fecha_hasta >= fecha_desde)
);

CREATE INDEX idx_periodo_contable_empresa ON periodo_contable(empresa_id);
CREATE INDEX idx_periodo_contable_rango ON periodo_contable(empresa_id, fecha_desde, fecha_hasta);

CREATE TABLE centro_costo (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT       NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    codigo      VARCHAR(20)  NOT NULL,
    nombre      VARCHAR(150) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, codigo)
);

CREATE INDEX idx_centro_costo_empresa ON centro_costo(empresa_id);

CREATE TABLE tercero_contable (
    id                BIGSERIAL PRIMARY KEY,
    empresa_id        BIGINT       NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    tipo_documento    VARCHAR(10)  NOT NULL,
    numero_documento  VARCHAR(30)  NOT NULL,
    nombre            VARCHAR(200) NOT NULL,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, tipo_documento, numero_documento)
);

CREATE INDEX idx_tercero_contable_empresa ON tercero_contable(empresa_id);

ALTER TABLE cuenta_contable
    ADD COLUMN cuenta_padre_id BIGINT REFERENCES cuenta_contable(id),
    ADD COLUMN naturaleza       VARCHAR(10) NOT NULL DEFAULT 'DEUDORA',
    ADD COLUMN nivel            INT         NOT NULL DEFAULT 1,
    ADD COLUMN version_puc      VARCHAR(10) NOT NULL DEFAULT '2025',
    ADD COLUMN acepta_auxiliar  BOOLEAN     NOT NULL DEFAULT FALSE;

DROP INDEX IF EXISTS uq_cuenta_contable_empresa_codigo;

CREATE UNIQUE INDEX uq_cuenta_contable_empresa_version_codigo
    ON cuenta_contable (COALESCE(empresa_id, 0), version_puc, codigo);

CREATE INDEX idx_cuenta_contable_padre ON cuenta_contable(cuenta_padre_id);
CREATE INDEX idx_cuenta_contable_version ON cuenta_contable(version_puc);

INSERT INTO cuenta_contable (empresa_id, codigo, nombre, tipo, version_puc)
SELECT NULL, v.codigo, v.nombre, v.tipo, '2025'
FROM (VALUES
    ('1', 'Activos', 'ACTIVO'),
    ('11', 'Efectivo y equivalentes al efectivo', 'ACTIVO'),
    ('13', 'Cuentas por cobrar', 'ACTIVO'),
    ('14', 'Inventarios', 'ACTIVO'),
    ('17', 'Propiedad, planta y equipo', 'ACTIVO'),
    ('19', 'Otros activos', 'ACTIVO'),
    ('110505', 'Caja general', 'ACTIVO'),
    ('110510', 'Bancos', 'ACTIVO'),
    ('130505', 'Clientes nacionales', 'ACTIVO'),
    ('135515', 'IVA descontable', 'ACTIVO'),
    ('135520', 'Retenciones y anticipos', 'ACTIVO'),
    ('139905', 'Deudores varios', 'ACTIVO'),
    ('140505', 'Mercancías en almacén', 'ACTIVO'),
    ('151405', 'Equipo de oficina', 'ACTIVO'),
    ('170505', 'Software', 'ACTIVO'),
    ('2', 'Pasivos', 'PASIVO'),
    ('22', 'Proveedores', 'PASIVO'),
    ('23', 'Cuentas por pagar', 'PASIVO'),
    ('24', 'Impuestos, gravámenes y tasas', 'PASIVO'),
    ('28', 'Anticipos de clientes', 'PASIVO'),
    ('29', 'Otros pasivos', 'PASIVO'),
    ('220505', 'Proveedores nacionales', 'PASIVO'),
    ('236505', 'Retención en la fuente', 'PASIVO'),
    ('236510', 'ReteIVA', 'PASIVO'),
    ('236515', 'ReteICA', 'PASIVO'),
    ('240805', 'IVA generado', 'PASIVO'),
    ('240810', 'IVA por pagar', 'PASIVO'),
    ('250405', 'Obligaciones laborales', 'PASIVO'),
    ('280505', 'Anticipos recibidos', 'PASIVO'),
    ('290505', 'Otros pasivos', 'PASIVO'),
    ('3', 'Patrimonio', 'PATRIMONIO'),
    ('31', 'Capital', 'PATRIMONIO'),
    ('32', 'Utilidades retenidas', 'PATRIMONIO'),
    ('39', 'Ajustes al patrimonio', 'PATRIMONIO'),
    ('310505', 'Capital social', 'PATRIMONIO'),
    ('320505', 'Utilidades retenidas', 'PATRIMONIO'),
    ('399999', 'Ajustes al patrimonio', 'PATRIMONIO'),
    ('4', 'Ingresos', 'INGRESO'),
    ('41', 'Ventas', 'INGRESO'),
    ('42', 'Otros ingresos', 'INGRESO'),
    ('413505', 'Ventas de bienes y servicios', 'INGRESO'),
    ('413515', 'Devoluciones sobre ventas', 'INGRESO'),
    ('419905', 'Otros ingresos operacionales', 'INGRESO'),
    ('5', 'Costo de ventas', 'EGRESO'),
    ('51', 'Costo de bienes vendidos', 'EGRESO'),
    ('52', 'Devoluciones y descuentos en compras', 'EGRESO'),
    ('519505', 'Compras de bienes y servicios', 'EGRESO'),
    ('529515', 'Devoluciones sobre compras', 'EGRESO'),
    ('6', 'Gastos operativos', 'EGRESO'),
    ('61', 'Gastos de personal', 'EGRESO'),
    ('62', 'Otros gastos', 'EGRESO'),
    ('619505', 'Sueldos y salarios', 'EGRESO'),
    ('629505', 'Gastos varios', 'EGRESO')
) AS v(codigo, nombre, tipo)
WHERE NOT EXISTS (
    SELECT 1 FROM cuenta_contable c
    WHERE c.empresa_id IS NULL AND c.version_puc = '2025' AND c.codigo = v.codigo
);

UPDATE cuenta_contable
SET naturaleza = CASE WHEN tipo IN ('PASIVO', 'PATRIMONIO', 'INGRESO') THEN 'ACREEDORA' ELSE 'DEUDORA' END,
    nivel = CASE WHEN length(codigo) <= 1 THEN 1 WHEN length(codigo) <= 2 THEN 2 WHEN length(codigo) <= 4 THEN 3 ELSE 4 END,
    acepta_auxiliar = codigo IN ('130505', '220505', '110510', '236505', '240805');

ALTER TABLE cuenta_contable
    ADD CONSTRAINT ck_cuenta_contable_naturaleza CHECK (naturaleza IN ('DEUDORA', 'ACREEDORA'));

UPDATE cuenta_contable c
SET cuenta_padre_id = p.id
FROM cuenta_contable p
WHERE p.empresa_id IS NULL
  AND p.version_puc = c.version_puc
  AND p.id <> c.id
  AND c.codigo LIKE p.codigo || '%'
  AND length(p.codigo) < length(c.codigo)
  AND length(p.codigo) = (
      SELECT max(length(p2.codigo))
      FROM cuenta_contable p2
      WHERE p2.empresa_id IS NULL
        AND p2.version_puc = c.version_puc
        AND p2.id <> c.id
        AND c.codigo LIKE p2.codigo || '%'
        AND length(p2.codigo) < length(c.codigo)
  );

DO $$
DECLARE restriccion record;
BEGIN
    FOR restriccion IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'asiento_contable'::regclass AND contype = 'u'
    LOOP
        EXECUTE format('ALTER TABLE asiento_contable DROP CONSTRAINT %I', restriccion.conname);
    END LOOP;
END $$;

ALTER TABLE asiento_contable
    ALTER COLUMN documento_tipo DROP NOT NULL,
    ALTER COLUMN documento_id DROP NOT NULL,
    ALTER COLUMN tipo_evento DROP NOT NULL,
    ADD COLUMN periodo_id       BIGINT REFERENCES periodo_contable(id),
    ADD COLUMN origen           VARCHAR(30),
    ADD COLUMN origen_id        BIGINT,
    ADD COLUMN reversa_de_id    BIGINT REFERENCES asiento_contable(id),
    ADD COLUMN centro_costo_id  BIGINT REFERENCES centro_costo(id),
    ADD COLUMN tercero_id       BIGINT REFERENCES tercero_contable(id);

CREATE UNIQUE INDEX uq_asiento_contable_documento
    ON asiento_contable (empresa_id, documento_tipo, documento_id, tipo_evento)
    WHERE documento_tipo IS NOT NULL AND documento_id IS NOT NULL;

CREATE UNIQUE INDEX uq_asiento_contable_origen
    ON asiento_contable (empresa_id, origen, origen_id, tipo_evento)
    WHERE origen IS NOT NULL;

CREATE INDEX idx_asiento_contable_periodo ON asiento_contable(periodo_id);
CREATE INDEX idx_asiento_contable_reversa ON asiento_contable(reversa_de_id);
CREATE INDEX idx_asiento_contable_centro_costo ON asiento_contable(centro_costo_id);
CREATE INDEX idx_asiento_contable_tercero ON asiento_contable(tercero_id);

ALTER TABLE asiento_contable
    ADD CONSTRAINT ck_asiento_contable_estado CHECK (estado IN ('BORRADOR', 'APROBADO', 'CONTABILIZADO', 'ANULADO'));

ALTER TABLE asiento_contable_linea
    ADD COLUMN centro_costo_id BIGINT REFERENCES centro_costo(id),
    ADD COLUMN tercero_id      BIGINT REFERENCES tercero_contable(id),
    ADD COLUMN base_imponible  NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN impuesto_codigo VARCHAR(20);

CREATE INDEX idx_asiento_linea_centro_costo ON asiento_contable_linea(centro_costo_id);
CREATE INDEX idx_asiento_linea_tercero ON asiento_contable_linea(tercero_id);

INSERT INTO periodo_contable (empresa_id, anio, mes, fecha_desde, fecha_hasta)
SELECT e.id,
       EXTRACT(YEAR FROM CURRENT_DATE)::INT,
       m.mes,
       make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INT, m.mes, 1),
       (make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INT, m.mes, 1) + INTERVAL '1 month - 1 day')::date
FROM empresa e
CROSS JOIN (SELECT generate_series(1, 12) AS mes) m
WHERE NOT EXISTS (
    SELECT 1 FROM periodo_contable p
    WHERE p.empresa_id = e.id
      AND p.anio = EXTRACT(YEAR FROM CURRENT_DATE)::INT
      AND p.mes = m.mes
);
