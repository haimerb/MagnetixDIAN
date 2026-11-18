-- V6: resolución de facturación para la empresa demo
-- Sin resolución la regla FE-005 emite la advertencia
-- "El documento no referencia una resolución de facturación".
UPDATE empresa_fe_config
SET resolucion_numero = '18764000523766',
    resolucion_fecha  = '2025-01-01'
WHERE empresa_id = (SELECT id FROM empresa WHERE nit = '900123456');
