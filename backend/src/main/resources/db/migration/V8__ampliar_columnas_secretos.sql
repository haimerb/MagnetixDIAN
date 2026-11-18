-- #18: ampliar columnas de secretos para almacenar el valor cifrado (AES-GCM + Base64).
ALTER TABLE empresa_fe_config
    ALTER COLUMN software_pin TYPE VARCHAR(300),
    ALTER COLUMN pac_api_key TYPE VARCHAR(600),
    ALTER COLUMN certificado_password TYPE VARCHAR(600);
