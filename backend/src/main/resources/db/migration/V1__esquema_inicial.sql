-- =============================================================================
-- V1: Esquema inicial MagnetixDIAN — Información Exógena DIAN
-- =============================================================================

SET TIME ZONE 'America/Bogota';

-- ---------------------------------------------------------------------------
-- Seguridad / Roles
-- ---------------------------------------------------------------------------
CREATE TABLE role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    description VARCHAR(150)
);

CREATE TABLE usuario (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(80)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    nombre        VARCHAR(150) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE usuario_role (
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    role_id    BIGINT NOT NULL REFERENCES role(id)    ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, role_id)
);

-- ---------------------------------------------------------------------------
-- Empresas (clientes)
-- ---------------------------------------------------------------------------
CREATE TABLE empresa (
    id                  BIGSERIAL PRIMARY KEY,
    nit                 VARCHAR(20)  NOT NULL UNIQUE,
    razon_social        VARCHAR(200) NOT NULL,
    regimen             VARCHAR(30)  NOT NULL DEFAULT 'ORDINARIO', -- ORDINARIO | SIMPLE
    gran_contribuyente  BOOLEAN      NOT NULL DEFAULT FALSE,
    naturaleza_entidad  VARCHAR(80),
    tipo_documento      VARCHAR(10)  NOT NULL DEFAULT 'NIT',
    direccion           VARCHAR(150),
    ciudad              VARCHAR(80),
    departamento        VARCHAR(80),
    codigo_dane         VARCHAR(10),
    email               VARCHAR(150),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

ALTER TABLE usuario ADD COLUMN empresa_id BIGINT REFERENCES empresa(id);

-- ---------------------------------------------------------------------------
-- Terceros
-- ---------------------------------------------------------------------------
CREATE TABLE tercero (
    id              BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    tipo_documento  VARCHAR(10) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    dv              INT,
    primer_apellido VARCHAR(100),
    segundo_apellido VARCHAR(100),
    primer_nombre   VARCHAR(100),
    otros_nombres   VARCHAR(100),
    razon_social    VARCHAR(200),
    direccion       VARCHAR(150),
    codigo_dane      VARCHAR(10),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, tipo_documento, numero_documento)
);

-- ---------------------------------------------------------------------------
-- Medios magnéticos (encabezado de reporte)
-- ---------------------------------------------------------------------------
CREATE TABLE medio_magnetico (
    id              BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT NOT NULL REFERENCES empresa(id),
    formato         VARCHAR(10) NOT NULL,          -- 1001
    version_formato VARCHAR(10) NOT NULL DEFAULT '10',
    anio_gravable   INT NOT NULL,
    estado          VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    -- BORRADOR | CARGADO | VALIDADO | VALIDADO_CON_ERRORES | XML_GENERADO | PRESENTADO
    usuario_creador BIGINT REFERENCES usuario(id),
    fecha_limite    DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, formato, anio_gravable)
);

-- ---------------------------------------------------------------------------
-- Operaciones (detalle formato 1001: pagos/abonos + retenciones)
-- ---------------------------------------------------------------------------
CREATE TABLE operacion (
    id                 BIGSERIAL PRIMARY KEY,
    medio_magnetico_id BIGINT  NOT NULL REFERENCES medio_magnetico(id) ON DELETE CASCADE,
    linea              INT     NOT NULL,
    tipo_documento     VARCHAR(10) NOT NULL,       -- NIT, CC, NITCE
    numero_identificacion VARCHAR(20) NOT NULL,    -- NIT del beneficiario
    dv                 INT,
    concepto           VARCHAR(10) NOT NULL,       -- código concepto DIAN
    valor_pago         NUMERIC(18,2) NOT NULL DEFAULT 0,
    retencion_renta    NUMERIC(18,2) NOT NULL DEFAULT 0,
    retencion_iva      NUMERIC(18,2) NOT NULL DEFAULT 0,
    retencion_ica      NUMERIC(18,2) NOT NULL DEFAULT 0,
    retencion_timbre   NUMERIC(18,2) NOT NULL DEFAULT 0,
    iva_pagado         NUMERIC(18,2) NOT NULL DEFAULT 0,
    iva_descontable    NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_gasto        NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_costo        NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_nc           NUMERIC(18,2) NOT NULL DEFAULT 0,
    cuantia_menor      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (medio_magnetico_id, linea)
);

CREATE INDEX idx_operacion_medio ON operacion(medio_magnetico_id);

-- ---------------------------------------------------------------------------
-- UVT por vigencia
-- ---------------------------------------------------------------------------
CREATE TABLE uvt_vigencia (
    anio     INT PRIMARY KEY,
    valor    NUMERIC(14,2) NOT NULL,
    resolucion VARCHAR(50),
    activo   BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------------------
-- Motor de reglas (catalogado, configurable)
-- ---------------------------------------------------------------------------
CREATE TABLE regla (
    id               BIGSERIAL PRIMARY KEY,
    codigo           VARCHAR(50) NOT NULL UNIQUE,
    nombre           VARCHAR(150) NOT NULL,
    descripcion      TEXT,
    severidad        VARCHAR(15) NOT NULL DEFAULT 'ERROR', -- ERROR | ADVERTENCIA | INFO
    activa           BOOLEAN NOT NULL DEFAULT TRUE,
    anio_gravable    INT,
    mensaje_template VARCHAR(500)
);

-- ---------------------------------------------------------------------------
-- Corridas de validación
-- ---------------------------------------------------------------------------
CREATE TABLE validacion (
    id                 BIGSERIAL PRIMARY KEY,
    medio_magnetico_id BIGINT NOT NULL REFERENCES medio_magnetico(id) ON DELETE CASCADE,
    inicio             TIMESTAMPTZ NOT NULL DEFAULT now(),
    fin                TIMESTAMPTZ,
    estado             VARCHAR(20) NOT NULL DEFAULT 'EN_PROCESO', -- EN_PROCESO | COMPLETADA
    errores_total      INT NOT NULL DEFAULT 0,
    advertencias_total INT NOT NULL DEFAULT 0,
    registros_validados INT NOT NULL DEFAULT 0
);

CREATE TABLE detalle_error (
    id           BIGSERIAL PRIMARY KEY,
    validacion_id BIGINT NOT NULL REFERENCES validacion(id) ON DELETE CASCADE,
    regla_id     BIGINT  REFERENCES regla(id),
    linea        INT,
    campo        VARCHAR(80),
    valor_esperado VARCHAR(120),
    valor_encontrado VARCHAR(120),
    mensaje      VARCHAR(500) NOT NULL,
    severidad    VARCHAR(15) NOT NULL
);

CREATE INDEX idx_detalle_validacion ON detalle_error(validacion_id);

-- ---------------------------------------------------------------------------
-- Calendario DIAN (fechas por rol NIT)
-- ---------------------------------------------------------------------------
CREATE TABLE calendario_dian (
    id           BIGSERIAL PRIMARY KEY,
    anio_gravable INT NOT NULL,
    anio_presentacion INT NOT NULL,
    tipo_reporte VARCHAR(30) NOT NULL,   -- GRAN_CONTRIBUYENTE | PERSONA_JURIDICA | PERSONA_NATURAL
    rango_nit_ini VARCHAR(2),
    rango_nit_fin VARCHAR(2),
    fecha_limite DATE NOT NULL
);

-- ---------------------------------------------------------------------------
-- Notificaciones
-- ---------------------------------------------------------------------------
CREATE TABLE notificacion (
    id            BIGSERIAL PRIMARY KEY,
    medio_magnetico_id BIGINT REFERENCES medio_magnetico(id) ON DELETE CASCADE,
    tipo          VARCHAR(30) NOT NULL,   -- RECORDATORIO_VENCIMIENTO
    destinatario  VARCHAR(150) NOT NULL,
    asunto        VARCHAR(200),
    enviado       BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_envio   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);