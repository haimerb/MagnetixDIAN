-- =============================================================================
-- V4: Facturación electrónica DIAN (FEV v1.9 / UBL 2.1)
-- Modelo multi-tenant: cada empresa emite sus documentos con su configuración.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Configuración de facturación electrónica por empresa
-- ---------------------------------------------------------------------------
CREATE TABLE empresa_fe_config (
    id                     BIGSERIAL PRIMARY KEY,
    empresa_id             BIGINT NOT NULL UNIQUE REFERENCES empresa(id) ON DELETE CASCADE,
    ambiente               SMALLINT NOT NULL DEFAULT 2,        -- 1 = producción, 2 = pruebas/habilitación
    software_id            VARCHAR(100),
    software_pin           VARCHAR(100),                       -- Clave técnica (ClTec)
    software_security_code VARCHAR(200),
    test_set_id            VARCHAR(100),
    resolucion_numero      VARCHAR(50),
    resolucion_fecha       DATE,
    prefijo                VARCHAR(10),
    rango_desde            BIGINT,
    rango_hasta            BIGINT,
    rango_vigencia_desde   DATE,
    rango_vigencia_hasta   DATE,
    canal                  VARCHAR(20) NOT NULL DEFAULT 'PAC', -- PAC | DIAN | SIMULADO
    pac_proveedor          VARCHAR(40),                        -- FACTUS | GENERICO
    pac_base_url           VARCHAR(300),
    pac_api_key            VARCHAR(300),
    certificado_nombre     VARCHAR(200),
    certificado_password   VARCHAR(200),
    certificado_path       VARCHAR(300),
    responsabilidad_fiscal VARCHAR(10) NOT NULL DEFAULT 'R-99-PN',
    codigo_municipio       VARCHAR(10),
    activo                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Consecutivos (numeración) por empresa + prefijo + tipo de documento
-- ---------------------------------------------------------------------------
CREATE TABLE consecutivo (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    prefijo        VARCHAR(10) NOT NULL,
    tipo_documento VARCHAR(10) NOT NULL DEFAULT '01',
    ultimo_numero  BIGINT NOT NULL DEFAULT 0,
    UNIQUE (empresa_id, prefijo, tipo_documento)
);

-- ---------------------------------------------------------------------------
-- Factura electrónica (encabezado)
-- ---------------------------------------------------------------------------
CREATE TABLE factura (
    id                     BIGSERIAL PRIMARY KEY,
    empresa_id             BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    tipo_documento         VARCHAR(10) NOT NULL DEFAULT '01',  -- 01 FE | 91 NC | 92 ND | 05 DS
    prefijo                VARCHAR(10),
    numero                 BIGINT NOT NULL,
    numero_completo        VARCHAR(30) NOT NULL,
    cufe                   VARCHAR(200),
    cude                   VARCHAR(200),
    ambiente               SMALLINT NOT NULL DEFAULT 2,
    fecha_emision          DATE NOT NULL,
    hora_emision           TIME NOT NULL,
    fecha_vencimiento      DATE,
    moneda                 VARCHAR(3) NOT NULL DEFAULT 'COP',
    forma_pago             VARCHAR(10) NOT NULL DEFAULT '1',
    medio_pago             VARCHAR(10) NOT NULL DEFAULT '10',
    tipo_operacion         VARCHAR(10) NOT NULL DEFAULT '10',
    adquirente_tipo_doc    VARCHAR(10) NOT NULL DEFAULT '13',
    adquirente_num_doc     VARCHAR(20) NOT NULL,
    adquirente_dv          INT,
    adquirente_nombre      VARCHAR(300) NOT NULL,
    adquirente_email       VARCHAR(200),
    adquirente_direccion   VARCHAR(200),
    adquirente_municipio   VARCHAR(10),
    adquirente_regimen     VARCHAR(20) DEFAULT 'ORDINARIO',
    adquirente_responsabilidad VARCHAR(10) DEFAULT 'R-99-PN',
    subtotal               NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_impuestos        NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_retenciones      NUMERIC(18,2) NOT NULL DEFAULT 0,
    total                  NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_pagar            NUMERIC(18,2) NOT NULL DEFAULT 0,
    observaciones          TEXT,
    estado                 VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    -- BORRADOR | GENERADO | FIRMADO | ENVIADO | ACEPTADO | RECHAZADO | ANULADA
    xml_generado           TEXT,
    xml_firmado            TEXT,
    pdf_path               VARCHAR(300),
    qr_url                 VARCHAR(600),
    respuesta_dian         TEXT,
    cufe_referencia        VARCHAR(200),
    numero_referencia      VARCHAR(30),
    fecha_validacion       TIMESTAMPTZ,
    usuario_creador        BIGINT REFERENCES usuario(id),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (empresa_id, numero_completo)
);

CREATE INDEX idx_factura_empresa ON factura(empresa_id);
CREATE INDEX idx_factura_estado ON factura(estado);

-- ---------------------------------------------------------------------------
-- Líneas de la factura
-- ---------------------------------------------------------------------------
CREATE TABLE factura_linea (
    id              BIGSERIAL PRIMARY KEY,
    factura_id      BIGINT NOT NULL REFERENCES factura(id) ON DELETE CASCADE,
    numero_linea    INT NOT NULL,
    codigo_producto VARCHAR(50),
    descripcion     VARCHAR(500) NOT NULL,
    cantidad        NUMERIC(18,6) NOT NULL DEFAULT 1,
    unidad_medida   VARCHAR(10) NOT NULL DEFAULT '94',
    valor_unitario  NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_descuento NUMERIC(18,2) NOT NULL DEFAULT 0,
    iva_pct         NUMERIC(5,2) NOT NULL DEFAULT 0,
    iva_valor       NUMERIC(18,2) NOT NULL DEFAULT 0,
    inc_pct         NUMERIC(5,2) NOT NULL DEFAULT 0,
    inc_valor       NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_linea     NUMERIC(18,2) NOT NULL DEFAULT 0,
    codigo_unspsc   VARCHAR(20),
    UNIQUE (factura_id, numero_linea)
);

CREATE INDEX idx_linea_factura ON factura_linea(factura_id);

-- ---------------------------------------------------------------------------
-- Impuestos y retenciones por documento
-- ---------------------------------------------------------------------------
CREATE TABLE factura_impuesto (
    id         BIGSERIAL PRIMARY KEY,
    factura_id BIGINT NOT NULL REFERENCES factura(id) ON DELETE CASCADE,
    tipo       VARCHAR(10) NOT NULL,   -- 01 IVA | 04 INC | 05 retefuente | 06 reteiva | 07 reteica
    nombre     VARCHAR(60),
    base       NUMERIC(18,2) NOT NULL DEFAULT 0,
    porcentaje NUMERIC(5,2) NOT NULL DEFAULT 0,
    valor      NUMERIC(18,2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_impuesto_factura ON factura_impuesto(factura_id);

-- ---------------------------------------------------------------------------
-- Eventos RADIAN (ApplicationResponse)
-- ---------------------------------------------------------------------------
CREATE TABLE factura_evento (
    id          BIGSERIAL PRIMARY KEY,
    factura_id  BIGINT NOT NULL REFERENCES factura(id) ON DELETE CASCADE,
    tipo_evento VARCHAR(10) NOT NULL,   -- 030 acuse | 031 reclamo | 032 recibo | 033 aceptación
    cude        VARCHAR(200),
    estado      VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    xml         TEXT,
    respuesta   TEXT,
    fecha       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_evento_factura ON factura_evento(factura_id);

-- ---------------------------------------------------------------------------
-- Catálogos DIAN (tipos de documento, tributos, unidades, etc.)
-- ---------------------------------------------------------------------------
CREATE TABLE catalogo_dian (
    id          BIGSERIAL PRIMARY KEY,
    tipo        VARCHAR(40) NOT NULL,   -- TIPO_DOCUMENTO | TRIBUTO | TIPO_OPERACION | UNIDAD | FORMA_PAGO | MEDIO_PAGO | RESPONSABILIDAD | TIPO_EVENTO
    codigo      VARCHAR(20) NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (tipo, codigo)
);
