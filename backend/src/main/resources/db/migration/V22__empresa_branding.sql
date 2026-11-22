CREATE TABLE empresa_branding (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL UNIQUE REFERENCES empresa(id) ON DELETE CASCADE,
    public_slug VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(120),
    logo_path TEXT,
    primary_color VARCHAR(20) NOT NULL DEFAULT '#0E5C55',
    accent_color VARCHAR(20) NOT NULL DEFAULT '#D98A2B',
    font_family VARCHAR(60) NOT NULL DEFAULT 'Roboto',
    invoice_footer TEXT,
    whatsapp_number VARCHAR(20),
    whatsapp_template TEXT,
    pwa_theme_color VARCHAR(20) NOT NULL DEFAULT '#0E5C55',
    pwa_background_color VARCHAR(20) NOT NULL DEFAULT '#FFFFFF',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_empresa_branding_slug ON empresa_branding(public_slug);
