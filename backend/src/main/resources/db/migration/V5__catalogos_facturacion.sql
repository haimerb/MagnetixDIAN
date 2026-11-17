-- =============================================================================
-- V5: Catálogos DIAN para facturación electrónica + configuración demo
-- =============================================================================

-- Tipos de documento electrónico
INSERT INTO catalogo_dian (tipo, codigo, descripcion) VALUES
    ('TIPO_DOCUMENTO', '01', 'Factura electrónica de venta'),
    ('TIPO_DOCUMENTO', '91', 'Nota crédito'),
    ('TIPO_DOCUMENTO', '92', 'Nota débito'),
    ('TIPO_DOCUMENTO', '05', 'Documento soporte en adquisiciones a no obligados');

-- Tributos
INSERT INTO catalogo_dian (tipo, codigo, descripcion) VALUES
    ('TRIBUTO', '01', 'IVA'),
    ('TRIBUTO', '04', 'INC'),
    ('TRIBUTO', '05', 'Retención en la fuente'),
    ('TRIBUTO', '06', 'Retención IVA'),
    ('TRIBUTO', '07', 'Retención ICA'),
    ('TRIBUTO', 'ZZ', 'Otros tributos');

-- Tipos de operación
INSERT INTO catalogo_dian (tipo, codigo, descripcion) VALUES
    ('TIPO_OPERACION', '09', 'Compras'),
    ('TIPO_OPERACION', '10', 'Estándar de factura de venta'),
    ('TIPO_OPERACION', '11', 'Mandatos'),
    ('TIPO_OPERACION', '12', 'Transporte'),
    ('TIPO_OPERACION', '20', 'Nota crédito que referencia factura'),
    ('TIPO_OPERACION', '21', 'Nota crédito sin referencia'),
    ('TIPO_OPERACION', '30', 'Nota débito que referencia factura'),
    ('TIPO_OPERACION', '31', 'Nota débito sin referencia');

-- Unidades de medida (subconjunto de uso común)
INSERT INTO catalogo_dian (tipo, codigo, descripcion) VALUES
    ('UNIDAD', '94', 'Unidad'),
    ('UNIDAD', 'EA', 'Cada uno'),
    ('UNIDAD', 'KGM', 'Kilogramo'),
    ('UNIDAD', 'MTR', 'Metro'),
    ('UNIDAD', 'LTR', 'Litro'),
    ('UNIDAD', 'HUR', 'Hora'),
    ('UNIDAD', 'DAY', 'Día'),
    ('UNIDAD', 'MON', 'Mes'),
    ('UNIDAD', 'ZZ', 'Servicio');

-- Forma de pago
INSERT INTO catalogo_dian (tipo, codigo, descripcion) VALUES
    ('FORMA_PAGO', '1', 'Contado'),
    ('FORMA_PAGO', '2', 'Crédito');

-- Medio de pago
INSERT INTO catalogo_dian (tipo, codigo, descripcion) VALUES
    ('MEDIO_PAGO', '10', 'Efectivo'),
    ('MEDIO_PAGO', '42', 'Consignación bancaria'),
    ('MEDIO_PAGO', '47', 'Transferencia débito bancaria'),
    ('MEDIO_PAGO', '48', 'Tarjeta crédito'),
    ('MEDIO_PAGO', '49', 'Tarjeta débito'),
    ('MEDIO_PAGO', 'ZZ', 'Otro');

-- Responsabilidad fiscal (subconjunto)
INSERT INTO catalogo_dian (tipo, codigo, descripcion) VALUES
    ('RESPONSABILIDAD', 'O-13', 'Gran contribuyente'),
    ('RESPONSABILIDAD', 'O-15', 'Autorretenedor'),
    ('RESPONSABILIDAD', 'O-23', 'Agente de retención IVA'),
    ('RESPONSABILIDAD', 'O-47', 'Régimen simple de tributación'),
    ('RESPONSABILIDAD', 'R-99-PN', 'No responsable');

-- Tipo de identificación fiscal (subconjunto)
INSERT INTO catalogo_dian (tipo, codigo, descripcion) VALUES
    ('TIPO_IDENTIFICACION', '11', 'Registro civil'),
    ('TIPO_IDENTIFICACION', '12', 'Tarjeta de identidad'),
    ('TIPO_IDENTIFICACION', '13', 'Cédula de ciudadanía'),
    ('TIPO_IDENTIFICACION', '21', 'Tarjeta de extranjería'),
    ('TIPO_IDENTIFICACION', '22', 'Cédula de extranjería'),
    ('TIPO_IDENTIFICACION', '31', 'NIT'),
    ('TIPO_IDENTIFICACION', '41', 'Pasaporte'),
    ('TIPO_IDENTIFICACION', '50', 'NIT otro país');

-- Eventos RADIAN
INSERT INTO catalogo_dian (tipo, codigo, descripcion) VALUES
    ('TIPO_EVENTO', '030', 'Acuse de recibo de la factura electrónica'),
    ('TIPO_EVENTO', '031', 'Reclamo de la factura electrónica'),
    ('TIPO_EVENTO', '032', 'Recibo del bien o prestación del servicio'),
    ('TIPO_EVENTO', '033', 'Aceptación expresa'),
    ('TIPO_EVENTO', '034', 'Aceptación tácita');

-- Configuración demo para la empresa demo (id resuelto por NIT)
INSERT INTO empresa_fe_config (empresa_id, ambiente, software_id, software_pin, prefijo,
                               rango_desde, rango_hasta, rango_vigencia_desde, rango_vigencia_hasta,
                               canal, responsabilidad_fiscal, codigo_municipio)
VALUES ((SELECT id FROM empresa WHERE nit = '900123456'),
        2, 'd1a1b1c1-0000-0000-0000-000000000001', '12345', 'FE',
        1, 5000, '2025-01-01', '2027-12-31',
        'SIMULADO', 'R-99-PN', '11001');
