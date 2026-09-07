-- =============================================================================
-- V2: Seed inicial — roles, UVT vigentes, reglas core, calendario 2026
-- =============================================================================

-- Roles
INSERT INTO role (code, description) VALUES
    ('ROLE_ADMIN',    'Administrador del sistema'),
    ('ROLE_CONTADOR', 'Contador / revisor fiscal'),
    ('ROLE_CLIENTE',  'Cliente final (empresa declarante)');

-- UVT: valores de referencia DIAN
INSERT INTO uvt_vigencia (anio, valor, resolucion) VALUES
    (2024, 47065.00, 'Resolución 000214 de 2023'),
    (2025, 49799.00, 'Resolución 000181 de 2024'),
    (2026, 52374.00, 'Resolución 000238 de 2025');

-- Reglas core del motor (regla.codigo es usada por los beans RuleValidator)
INSERT INTO regla (codigo, nombre, descripcion, severidad, mensaje_template) VALUES
    ('NIT_DIGITO_VERIFICACION', 'Validación dígito de verificación NIT',
     'Valida que el dígito de verificación del NIT sea correcto según algoritmo DIAN (módulo 11).',
     'ERROR', 'NIT {valor} con dígito de verificación inválido.'),
    ('CONCEPTO_VALIDO', 'Concepto válido para el formato',
     'Verifica que el código de concepto pertenezca al catálogo vigente del formato 1001.',
     'ERROR', 'Concepto {valor} no existe en el catálogo del formato.'),
    ('TOPE_UVT_PAGOS', 'Tope UVT para reporte de pagos',
     'Pagos menores a la cuantía menor (3 UVT) se reportan de forma agregada como cuantías menores.',
     'ADVERTENCIA', 'Pago por debajo del tope de 3 UVT, debe reportarse como cuantía menor.'),
    ('OBLIGATORIEDAD_CAMPO', 'Campos obligatorios',
     'Verifica presencia y formato de campos obligatorios del registro (NIT, concepto, valor).',
     'ERROR', 'Campo obligatorio {campo} ausente o con formato inválido.'),
    ('CONSISTENCIA_VALORES', 'Consistencia de valores',
     'Valida que las retenciones no superen el valor pagado y que los totales sean coherentes.',
     'ERROR', 'Valor inconsistente: {campo} supera el valor del pago.'),
    ('FORMATO_NIT', 'Formato del NIT',
     'Valida formato alfanumérico del NIT y rangos razonables.',
     'ADVERTENCIA', 'NIT {valor} con formato inusual, verificar.');

-- Calendario DIAN año gravable 2025 (presentación 2026)
-- GGCC: 28-abr a 13-may-2026 según último dígito NIT
-- PJ/PN: 14-may a 12-jun-2026 según últimos dos dígitos NIT
INSERT INTO calendario_dian (anio_gravable, anio_presentacion, tipo_reporte, rango_nit_ini, rango_nit_fin, fecha_limite) VALUES
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '0', '2026-04-28'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '1', '2026-04-29'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '2', '2026-04-30'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '3', '2026-05-01'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '4', '2026-05-04'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '5', '2026-05-05'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '6', '2026-05-06'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '7', '2026-05-07'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '8', '2026-05-08'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '9', '2026-05-09'),
    (2025, 2026, 'GRAN_CONTRIBUYENTE', null, '',  '2026-05-13');

-- Personas jurídicas y naturales según dos últimos dígitos NIT
INSERT INTO calendario_dian (anio_gravable, anio_presentacion, tipo_reporte, rango_nit_ini, rango_nit_fin, fecha_limite) VALUES
    (2025, 2026, 'PERSONA_JURIDICA', '00', '04', '2026-05-14'),
    (2025, 2026, 'PERSONA_JURIDICA', '05', '09', '2026-05-15'),
    (2025, 2026, 'PERSONA_JURIDICA', '10', '14', '2026-05-18'),
    (2025, 2026, 'PERSONA_JURIDICA', '15', '19', '2026-05-19'),
    (2025, 2026, 'PERSONA_JURIDICA', '20', '24', '2026-05-20'),
    (2025, 2026, 'PERSONA_JURIDICA', '25', '29', '2026-05-21'),
    (2025, 2026, 'PERSONA_JURIDICA', '30', '34', '2026-05-22'),
    (2025, 2026, 'PERSONA_JURIDICA', '35', '39', '2026-05-25'),
    (2025, 2026, 'PERSONA_JURIDICA', '40', '44', '2026-05-26'),
    (2025, 2026, 'PERSONA_JURIDICA', '45', '49', '2026-05-27'),
    (2025, 2026, 'PERSONA_JURIDICA', '50', '54', '2026-05-28'),
    (2025, 2026, 'PERSONA_JURIDICA', '55', '59', '2026-05-29'),
    (2025, 2026, 'PERSONA_JURIDICA', '60', '64', '2026-06-01'),
    (2025, 2026, 'PERSONA_JURIDICA', '65', '69', '2026-06-02'),
    (2025, 2026, 'PERSONA_JURIDICA', '70', '74', '2026-06-03'),
    (2025, 2026, 'PERSONA_JURIDICA', '75', '79', '2026-06-04'),
    (2025, 2026, 'PERSONA_JURIDICA', '80', '84', '2026-06-05'),
    (2025, 2026, 'PERSONA_JURIDICA', '85', '89', '2026-06-08'),
    (2025, 2026, 'PERSONA_JURIDICA', '90', '94', '2026-06-09'),
    (2025, 2026, 'PERSONA_JURIDICA', '95', '99', '2026-06-10'),
    (2025, 2026, 'PERSONA_JURIDICA', '00', '99', '2026-06-12');