INSERT INTO cuenta_contable (empresa_id, codigo, nombre, tipo, version_puc, naturaleza, nivel, acepta_auxiliar, activa, cuenta_padre_id)
SELECT NULL, v.codigo, v.nombre, v.tipo, versiones.version_puc, v.naturaleza, v.nivel, FALSE, TRUE, NULL
FROM (VALUES
    ('159215', 'Depreciación acumulada - equipo de oficina', 'ACTIVO', 'ACREEDORA', 4),
    ('516015', 'Depreciación de equipo de oficina', 'EGRESO', 'DEUDORA', 4)
) AS v(codigo, nombre, tipo, naturaleza, nivel)
CROSS JOIN (VALUES ('2025'), ('NIIF_FULL'), ('NIIF_PYMES')) AS versiones(version_puc)
WHERE NOT EXISTS (
    SELECT 1 FROM cuenta_contable c
    WHERE c.empresa_id IS NULL AND c.version_puc = versiones.version_puc AND c.codigo = v.codigo
);
