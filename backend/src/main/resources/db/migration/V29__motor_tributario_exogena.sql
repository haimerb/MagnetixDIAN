DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'empresa' AND column_name = 'regimen_niif'
    ) THEN
        ALTER TABLE empresa ADD COLUMN regimen_niif VARCHAR(20) NOT NULL DEFAULT 'NIIF_PYMES';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tercero_contable' AND column_name = 'dv'
    ) THEN
        ALTER TABLE tercero_contable ADD COLUMN dv VARCHAR(2);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tercero_contable' AND column_name = 'municipio'
    ) THEN
        ALTER TABLE tercero_contable ADD COLUMN municipio VARCHAR(100);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tercero_contable' AND column_name = 'pais'
    ) THEN
        ALTER TABLE tercero_contable ADD COLUMN pais VARCHAR(3) DEFAULT 'CO';
    END IF;
END $$;

INSERT INTO cuenta_contable (empresa_id, codigo, nombre, tipo, activa, naturaleza, nivel, version_puc, acepta_auxiliar)
SELECT NULL, c.codigo, c.nombre, c.tipo, c.activa, c.naturaleza, c.nivel, v.version, c.acepta_auxiliar
FROM cuenta_contable c
CROSS JOIN (VALUES ('NIIF_FULL'), ('NIIF_PYMES')) AS v(version)
WHERE c.empresa_id IS NULL
  AND c.version_puc = '2025'
  AND NOT EXISTS (
      SELECT 1 FROM cuenta_contable x
      WHERE x.empresa_id IS NULL AND x.version_puc = v.version AND x.codigo = c.codigo
  );

UPDATE cuenta_contable c
SET cuenta_padre_id = p.id
FROM cuenta_contable p
WHERE p.empresa_id IS NULL
  AND p.version_puc IN ('NIIF_FULL', 'NIIF_PYMES')
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

CREATE TABLE IF NOT EXISTS dian_concepto (
    id               BIGSERIAL PRIMARY KEY,
    ano              INT           NOT NULL,
    formato          VARCHAR(10)   NOT NULL,
    codigo_concepto  VARCHAR(20)   NOT NULL,
    descripcion      VARCHAR(300)  NOT NULL,
    tipo_tercero     VARCHAR(20),
    tipo_documento   VARCHAR(10),
    valor_minimo     NUMERIC(18,2) NOT NULL DEFAULT 0,
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (ano, formato, codigo_concepto)
);

CREATE INDEX IF NOT EXISTS idx_dian_concepto_catalogo ON dian_concepto(ano, formato, activo);

CREATE TABLE IF NOT EXISTS dian_cuenta_concepto (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    ano           INT           NOT NULL,
    cuenta_codigo VARCHAR(10)   NOT NULL,
    concepto_id   BIGINT        NOT NULL REFERENCES dian_concepto(id) ON DELETE CASCADE,
    activo        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_dian_cuenta_concepto_global
    ON dian_cuenta_concepto (COALESCE(empresa_id, 0), ano, cuenta_codigo, concepto_id);

CREATE INDEX IF NOT EXISTS idx_dian_cuenta_concepto_cuenta ON dian_cuenta_concepto(ano, cuenta_codigo);

CREATE TABLE IF NOT EXISTS movimiento_tributario (
    id                       BIGSERIAL PRIMARY KEY,
    empresa_id               BIGINT        NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    ano                      INT           NOT NULL,
    asiento_id               BIGINT        REFERENCES asiento_contable(id) ON DELETE SET NULL,
    asiento_linea_id         BIGINT        REFERENCES asiento_contable_linea(id) ON DELETE SET NULL,
    tercero_id               BIGINT        REFERENCES tercero_contable(id) ON DELETE SET NULL,
    cuenta_codigo            VARCHAR(10)   NOT NULL,
    concepto_id              BIGINT        REFERENCES dian_concepto(id),
    valor                    NUMERIC(18,2) NOT NULL,
    fecha                    DATE          NOT NULL,
    tipo_documento           VARCHAR(10),
    numero_documento         VARCHAR(30),
    numero_documento_dv      VARCHAR(2),
    municipio                VARCHAR(100),
    pais                     VARCHAR(3),
    origen_tipo_documento    VARCHAR(30),
    origen_numero_documento  VARCHAR(50),
    origen                   VARCHAR(30)   NOT NULL,
    procesado                BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_movimiento_tributario_origen
    ON movimiento_tributario (
        empresa_id, ano, COALESCE(asiento_id, 0), cuenta_codigo,
        COALESCE(concepto_id, 0), origen, COALESCE(numero_documento, '')
    );

CREATE INDEX IF NOT EXISTS idx_movimiento_tributario_agg ON movimiento_tributario(empresa_id, ano, concepto_id, tercero_id);

CREATE TABLE IF NOT EXISTS exogena_validacion (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT        NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    ano            INT           NOT NULL,
    formato        VARCHAR(10)   NOT NULL,
    generacion_id  BIGINT,
    severidad      VARCHAR(12)   NOT NULL CHECK (severidad IN ('BLOQUEANTE', 'ADVERTENCIA')),
    codigo         VARCHAR(40)   NOT NULL,
    mensaje        VARCHAR(500)  NOT NULL,
    tercero_id     BIGINT,
    concepto_id    BIGINT,
    cuenta_codigo  VARCHAR(10),
    identificador  VARCHAR(120),
    procesado      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_exogena_validacion_generacion ON exogena_validacion(generacion_id);

CREATE TABLE IF NOT EXISTS exogena_generacion (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT        NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    ano                   INT           NOT NULL,
    formato               VARCHAR(10)   NOT NULL,
    estado                VARCHAR(15)   NOT NULL CHECK (estado IN ('REGISTRADO', 'GENERADO', 'ERROR')),
    incluir_advertencias  BOOLEAN       NOT NULL DEFAULT FALSE,
    total_renglones       INT           NOT NULL DEFAULT 0,
    total_validaciones    INT           NOT NULL DEFAULT 0,
    prevalidacion_oficial BOOLEAN       NOT NULL DEFAULT FALSE,
    xml                   TEXT,
    excel                 BYTEA,
    mensaje               VARCHAR(500),
    creado_por            VARCHAR(100),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    finished_at           TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_exogena_generacion_empresa ON exogena_generacion(empresa_id, ano, formato);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_exogena_validacion_generacion'
    ) THEN
        ALTER TABLE exogena_validacion
            ADD CONSTRAINT fk_exogena_validacion_generacion
            FOREIGN KEY (generacion_id) REFERENCES exogena_generacion(id) ON DELETE CASCADE;
    END IF;
END $$;

INSERT INTO dian_concepto (ano, formato, codigo_concepto, descripcion, tipo_tercero, tipo_documento, valor_minimo)
SELECT anio.ano, '1001', v.codigo, v.descripcion, v.tipo_tercero, v.tipo_documento, v.valor_minimo
FROM generate_series(2025, EXTRACT(YEAR FROM CURRENT_DATE)::int + 1) AS anio(ano)
CROSS JOIN (VALUES
    ('1001', 'Ingresos por ventas y servicios', 'JURIDICA', 'NIT', 0),
    ('1002', 'Devoluciones, rebajas y descuentos', 'JURIDICA', 'NIT', 0),
    ('1003', 'Compras y gastos deducibles', 'JURIDICA', 'NIT', 0),
    ('1004', 'IVA generado', 'JURIDICA', 'NIT', 0),
    ('1005', 'IVA descontable', 'JURIDICA', 'NIT', 0),
    ('1006', 'Retenciones practicadas', 'JURIDICA', 'NIT', 0),
    ('1007', 'Retenciones recibidas', 'JURIDICA', 'NIT', 0)
) AS v(codigo, descripcion, tipo_tercero, tipo_documento, valor_minimo)
WHERE NOT EXISTS (
    SELECT 1 FROM dian_concepto c
    WHERE c.ano = anio.ano AND c.formato = '1001' AND c.codigo_concepto = v.codigo
);

INSERT INTO dian_concepto (ano, formato, codigo_concepto, descripcion, tipo_tercero, tipo_documento, valor_minimo)
SELECT anio.ano, f.formato, '0001', 'Concepto genérico ' || f.formato, NULL, NULL, 0
FROM generate_series(2025, EXTRACT(YEAR FROM CURRENT_DATE)::int + 1) AS anio(ano)
CROSS JOIN (VALUES
    ('1003'), ('1005'), ('1006'), ('1007'), ('1008'), ('1009'),
    ('2276'), ('1125'), ('2278'), ('5253')
) AS f(formato)
WHERE NOT EXISTS (
    SELECT 1 FROM dian_concepto c
    WHERE c.ano = anio.ano AND c.formato = f.formato AND c.codigo_concepto = '0001'
);

INSERT INTO dian_cuenta_concepto (empresa_id, ano, cuenta_codigo, concepto_id, activo)
SELECT NULL, c.ano, m.cuenta_codigo, c.id, TRUE
FROM dian_concepto c
JOIN (VALUES
    ('413505', '1001'), ('419905', '1001'),
    ('413515', '1002'), ('529515', '1002'),
    ('519505', '1003'), ('629505', '1003'),
    ('240805', '1004'), ('240810', '1004'),
    ('135515', '1005'),
    ('236505', '1006'), ('236510', '1006'), ('236515', '1006'),
    ('135520', '1007')
) AS m(cuenta_codigo, codigo_concepto) ON m.codigo_concepto = c.codigo_concepto
WHERE c.formato = '1001'
  AND NOT EXISTS (
      SELECT 1 FROM dian_cuenta_concepto dcc
      WHERE dcc.empresa_id IS NULL
        AND dcc.ano = c.ano
        AND dcc.cuenta_codigo = m.cuenta_codigo
        AND dcc.concepto_id = c.id
  );
