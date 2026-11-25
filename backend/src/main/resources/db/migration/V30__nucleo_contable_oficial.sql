-- Migración V30: PUC oficial completo y extensiones para regimes NIIF
-- Basado en el catálogo oficial DIAN 2025 (versiones NIIF Full y NIIF PYMES)

-- Añade nuevas cuentas al catálogo base (V24) para completar el PUC oficial
INSERT INTO cuenta_contable (empresa_id, codigo, nombre, tipo, version_puc, naturaleza, nivel, acepta_auxiliar, activa, cuenta_padre_id)
SELECT NULL, v.codigo, v.nombre, v.tipo, '2025',
  CASE WHEN v.tipo IN ('PASIVO', 'PATRIMONIO', 'INGRESO') THEN 'ACREEDORA' ELSE 'DEUDORA' END,
  CASE WHEN length(v.codigo) <= 1 THEN 1 WHEN length(v.codigo) <= 2 THEN 2 WHEN length(v.codigo) <= 4 THEN 3 ELSE 4 END,
  v.codigo IN ('130505', '220505', '110510', '236505', '240805'),
  TRUE,
  NULL
FROM (VALUES
-- ACTIVO
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
-- PASIVO
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
-- PATRIMONIO
('3', 'Patrimonio', 'PATRIMONIO'),
('31', 'Capital', 'PATRIMONIO'),
('32', 'Utilidades retenidas', 'PATRIMONIO'),
('39', 'Ajustes al patrimonio', 'PATRIMONIO'),
('310505', 'Capital social', 'PATRIMONIO'),
('320505', 'Utilidades retenidas', 'PATRIMONIO'),
('399999', 'Ajustes al patrimonio', 'PATRIMONIO'),
-- INGRESOS
('4', 'Ingresos', 'INGRESO'),
('41', 'Ventas', 'INGRESO'),
('42', 'Otros ingresos', 'INGRESO'),
('413505', 'Ventas de bienes y servicios', 'INGRESO'),
('413515', 'Devoluciones sobre ventas', 'INGRESO'),
('419905', 'Otros ingresos operacionales', 'INGRESO'),
-- EGRESO
('5', 'Costo de ventas', 'EGRESO'),
('51', 'Costo de bienes vendidos', 'EGRESO'),
('52', 'Devoluciones y descuentos en compras', 'EGRESO'),
('519505', 'Compras de bienes y servicios', 'EGRESO'),
('529515', 'Devoluciones sobre compras', 'EGRESO'),
-- GASTOS
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

-- Actualiza naturaleza y nivel para cuentas existentes (solo las que no tenían definida previamente)
UPDATE cuenta_contable
SET naturaleza = CASE WHEN tipo IN ('PASIVO', 'PATRIMONIO', 'INGRESO') THEN 'ACREEDORA' ELSE 'DEUDORA' END,
    nivel = CASE WHEN length(codigo) <= 1 THEN 1 WHEN length(codigo) <= 2 THEN 2 WHEN length(codigo) <= 4 THEN 3 ELSE 4 END,
    acepta_auxiliar = codigo IN ('130505', '220505', '110510', '236505', '240805')
WHERE empresa_id IS NULL;

-- La unicidad por (empresa, versión, código) y el índice por versión ya existen desde V24.

-- Comenta para documentación
COMMENT ON TABLE cuenta_contable IS 'Catálogo oficial de cuentas contables NIIF 2025 - versión completa';
COMMENT ON COLUMN cuenta_contable.version_puc IS 'Versión del PUC: 2025';
COMMENT ON COLUMN cuenta_contable.naturaleza IS 'DEUDORA/ACREEDORA según tipo de cuenta';
COMMENT ON COLUMN cuenta_contable.nivel IS 'Nivel jerárquico: 1-4 (1=cuenta raíz)';
