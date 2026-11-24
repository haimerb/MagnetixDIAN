CREATE TABLE erp_orden_venta (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    numero VARCHAR(40) NOT NULL,
    cliente_id VARCHAR(80),
    cliente_nombre VARCHAR(200),
    fecha DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuestos NUMERIC(18,2) NOT NULL DEFAULT 0,
    total NUMERIC(18,2) NOT NULL DEFAULT 0,
    moneda VARCHAR(10) NOT NULL DEFAULT 'COP',
    observaciones VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    CONSTRAINT uq_erp_orden_venta_empresa_numero UNIQUE (empresa_id, numero)
);

CREATE TABLE erp_pedido (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    orden_venta_id BIGINT REFERENCES erp_orden_venta(id),
    numero VARCHAR(40) NOT NULL,
    cliente_id VARCHAR(80),
    cliente_nombre VARCHAR(200),
    fecha DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuestos NUMERIC(18,2) NOT NULL DEFAULT 0,
    total NUMERIC(18,2) NOT NULL DEFAULT 0,
    factura_dian_id VARCHAR(120),
    observaciones VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    CONSTRAINT uq_erp_pedido_empresa_numero UNIQUE (empresa_id, numero)
);

CREATE TABLE erp_compra (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    numero VARCHAR(40) NOT NULL,
    proveedor_id VARCHAR(80),
    proveedor_nombre VARCHAR(200),
    fecha DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
    impuestos NUMERIC(18,2) NOT NULL DEFAULT 0,
    total NUMERIC(18,2) NOT NULL DEFAULT 0,
    observaciones VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    CONSTRAINT uq_erp_compra_empresa_numero UNIQUE (empresa_id, numero)
);

CREATE TABLE erp_recepcion (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    compra_id BIGINT NOT NULL REFERENCES erp_compra(id),
    numero VARCHAR(40) NOT NULL,
    fecha DATE NOT NULL,
    bodega VARCHAR(80) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    total NUMERIC(18,2) NOT NULL DEFAULT 0,
    observaciones VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    CONSTRAINT uq_erp_recepcion_empresa_numero UNIQUE (empresa_id, numero)
);

CREATE TABLE erp_recepcion_linea (
    id BIGSERIAL PRIMARY KEY,
    recepcion_id BIGINT NOT NULL REFERENCES erp_recepcion(id) ON DELETE CASCADE,
    producto_id VARCHAR(80) NOT NULL,
    descripcion VARCHAR(300),
    cantidad NUMERIC(18,4) NOT NULL,
    costo_unitario NUMERIC(18,4) NOT NULL,
    total NUMERIC(18,2) NOT NULL
);

CREATE TABLE erp_inventario_movimiento (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    tipo VARCHAR(20) NOT NULL,
    bodega VARCHAR(80) NOT NULL,
    producto_id VARCHAR(80) NOT NULL,
    descripcion VARCHAR(300),
    cantidad NUMERIC(18,4) NOT NULL,
    costo_unitario NUMERIC(18,4) NOT NULL,
    costo_promedio NUMERIC(18,4) NOT NULL,
    existencia_anterior NUMERIC(18,4) NOT NULL DEFAULT 0,
    existencia_nueva NUMERIC(18,4) NOT NULL DEFAULT 0,
    fecha DATE NOT NULL,
    referencia_tipo VARCHAR(40),
    referencia_id VARCHAR(80),
    estado VARCHAR(30) NOT NULL,
    observaciones VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120)
);

CREATE TABLE erp_inventario_saldo (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    bodega VARCHAR(80) NOT NULL,
    producto_id VARCHAR(80) NOT NULL,
    existencia NUMERIC(18,4) NOT NULL DEFAULT 0,
    costo_promedio NUMERIC(18,4) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_erp_inventario_saldo UNIQUE (empresa_id, bodega, producto_id)
);

CREATE TABLE erp_activo (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    codigo VARCHAR(40) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    categoria VARCHAR(80),
    fecha_alta DATE NOT NULL,
    valor_adquisicion NUMERIC(18,2) NOT NULL,
    valor_residual NUMERIC(18,2) NOT NULL DEFAULT 0,
    vida_util_meses INT NOT NULL,
    depreciacion_acumulada NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_libros NUMERIC(18,2) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_baja DATE,
    observaciones VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    CONSTRAINT uq_erp_activo_empresa_codigo UNIQUE (empresa_id, codigo)
);

CREATE TABLE erp_depreciacion (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    activo_id BIGINT NOT NULL REFERENCES erp_activo(id),
    periodo VARCHAR(7) NOT NULL,
    fecha DATE NOT NULL,
    valor NUMERIC(18,2) NOT NULL,
    depreciacion_acumulada NUMERIC(18,2) NOT NULL,
    valor_libros NUMERIC(18,2) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    CONSTRAINT uq_erp_depreciacion_activo_periodo UNIQUE (activo_id, periodo)
);

CREATE TABLE erp_cuenta_bancaria (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    banco VARCHAR(120) NOT NULL,
    numero_cuenta VARCHAR(80) NOT NULL,
    tipo_cuenta VARCHAR(30) NOT NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'COP',
    saldo NUMERIC(18,2) NOT NULL DEFAULT 0,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    CONSTRAINT uq_erp_cuenta_bancaria UNIQUE (empresa_id, banco, numero_cuenta)
);

CREATE TABLE erp_movimiento_tesoreria (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    cuenta_bancaria_id BIGINT NOT NULL REFERENCES erp_cuenta_bancaria(id),
    tipo VARCHAR(30) NOT NULL,
    fecha DATE NOT NULL,
    valor NUMERIC(18,2) NOT NULL,
    saldo_anterior NUMERIC(18,2) NOT NULL,
    saldo_nuevo NUMERIC(18,2) NOT NULL,
    referencia_tipo VARCHAR(40),
    referencia_id VARCHAR(80),
    estado VARCHAR(30) NOT NULL,
    descripcion VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120)
);

CREATE TABLE erp_pago_proveedor (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    compra_id BIGINT REFERENCES erp_compra(id),
    cuenta_bancaria_id BIGINT NOT NULL REFERENCES erp_cuenta_bancaria(id),
    proveedor_id VARCHAR(80),
    proveedor_nombre VARCHAR(200),
    fecha DATE NOT NULL,
    valor NUMERIC(18,2) NOT NULL,
    medio_pago VARCHAR(40),
    referencia VARCHAR(120),
    estado VARCHAR(30) NOT NULL,
    observaciones VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120)
);

CREATE TABLE erp_cobro_cliente (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    pedido_id BIGINT REFERENCES erp_pedido(id),
    cuenta_bancaria_id BIGINT NOT NULL REFERENCES erp_cuenta_bancaria(id),
    cliente_id VARCHAR(80),
    cliente_nombre VARCHAR(200),
    fecha DATE NOT NULL,
    valor NUMERIC(18,2) NOT NULL,
    medio_pago VARCHAR(40),
    referencia VARCHAR(120),
    estado VARCHAR(30) NOT NULL,
    observaciones VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120)
);

CREATE TABLE erp_integracion_config (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    proveedor VARCHAR(40) NOT NULL,
    tipo VARCHAR(40) NOT NULL DEFAULT 'ERP',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    credenciales_cifradas TEXT,
    configuracion TEXT,
    estado VARCHAR(30) NOT NULL,
    ultimo_error VARCHAR(1000),
    ultima_sincronizacion TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    CONSTRAINT uq_erp_integracion_config UNIQUE (empresa_id, proveedor, tipo)
);
CREATE INDEX idx_erp_integracion_config_empresa ON erp_integracion_config(empresa_id);

CREATE INDEX idx_erp_orden_venta_empresa ON erp_orden_venta(empresa_id);
CREATE INDEX idx_erp_pedido_empresa ON erp_pedido(empresa_id);
CREATE INDEX idx_erp_compra_empresa ON erp_compra(empresa_id);
CREATE INDEX idx_erp_recepcion_empresa ON erp_recepcion(empresa_id);
CREATE INDEX idx_erp_inv_mov_empresa ON erp_inventario_movimiento(empresa_id, bodega, producto_id);
CREATE INDEX idx_erp_inv_saldo_empresa ON erp_inventario_saldo(empresa_id, bodega, producto_id);
CREATE INDEX idx_erp_activo_empresa ON erp_activo(empresa_id);
CREATE INDEX idx_erp_cuenta_bancaria_empresa ON erp_cuenta_bancaria(empresa_id);
CREATE INDEX idx_erp_tesoreria_empresa ON erp_movimiento_tesoreria(empresa_id);
CREATE INDEX idx_erp_pago_proveedor_empresa ON erp_pago_proveedor(empresa_id);
CREATE INDEX idx_erp_cobro_cliente_empresa ON erp_cobro_cliente(empresa_id);
