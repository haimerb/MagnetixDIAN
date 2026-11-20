ALTER TABLE usuario ADD COLUMN IF NOT EXISTS terminos_aceptados_en TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_usuario_empresa ON usuario(empresa_id);
