-- =============================================================================
-- V3: Seed de usuario administrador y empresa demo
-- =============================================================================

-- Empresa demo
INSERT INTO empresa (nit, razon_social, regimen, gran_contribuyente, tipo_documento,
                     direccion, ciudad, departamento, codigo_dane, email)
VALUES ('900123456', 'MAGNETIXDIAN SAS', 'ORDINARIO', FALSE, 'NIT',
        'Calle 100 # 15 - 20', 'Bogotá', 'Bogotá D.C.', '11001', 'demo@magnetixdian.co');

-- Usuario administrador (password: admin123, hash BCrypt)
INSERT INTO usuario (username, password_hash, email, nombre, enabled, empresa_id)
VALUES ('admin', '$2a$10$bXjw4jSQvfgVRPT2keYaqOess5HuzN/y8XBOiMMKNQ/isif8k05w2',
        'admin@magnetixdian.co', 'Administrador Sistema', TRUE, (SELECT id FROM empresa WHERE nit = '900123456'));

-- Roles del admin
INSERT INTO usuario_role (usuario_id, role_id)
SELECT u.id, r.id FROM usuario u, role r
WHERE u.username = 'admin' AND r.code IN ('ROLE_ADMIN', 'ROLE_CONTADOR', 'ROLE_CLIENTE');