-- =============================================================================
-- V11: Planes y suscripciones (Fase 3 — billing)
--   - Catálogo de planes con límites de uso (0 = ilimitado).
--   - Suscripción activa por empresa; el proveedor identifica la pasarela.
-- =============================================================================

CREATE TABLE plan (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(30) NOT NULL UNIQUE,
    nombre              VARCHAR(80) NOT NULL,
    descripcion         VARCHAR(300),
    precio_mensual      NUMERIC(12,2) NOT NULL DEFAULT 0,
    moneda              VARCHAR(3) NOT NULL DEFAULT 'COP',
    limite_facturas_mes INT NOT NULL DEFAULT 0,
    limite_usuarios     INT NOT NULL DEFAULT 0,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    orden               INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE suscripcion (
    id                 BIGSERIAL PRIMARY KEY,
    empresa_id         BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    plan_id            BIGINT NOT NULL REFERENCES plan(id),
    estado             VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    proveedor          VARCHAR(20) NOT NULL DEFAULT 'SIMULADO',
    referencia_externa VARCHAR(120),
    periodo_inicio     DATE NOT NULL DEFAULT CURRENT_DATE,
    periodo_fin        DATE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_suscripcion_empresa ON suscripcion(empresa_id, estado);

INSERT INTO plan (codigo, nombre, descripcion, precio_mensual, limite_facturas_mes, limite_usuarios, orden) VALUES
('FREE',       'Gratis',       'Hasta 50 documentos al mes y 1 usuario.',            0,      50,   1, 1),
('PRO',        'Profesional',  'Hasta 1.000 documentos al mes y 5 usuarios.',   99000,    1000,   5, 2),
('ENTERPRISE', 'Empresa',      'Documentos y usuarios ilimitados.',            299000,       0,   0, 3);

-- Suscripción demo (empresa MAGNETIXDIAN SAS) en plan Profesional.
INSERT INTO suscripcion (empresa_id, plan_id, estado, proveedor, periodo_inicio)
SELECT e.id, p.id, 'ACTIVA', 'SIMULADO', CURRENT_DATE
FROM empresa e, plan p
WHERE e.nit = '900123456' AND p.codigo = 'PRO';
