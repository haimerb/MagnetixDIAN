-- #16: envío de la factura electrónica al adquirente por correo (flag por empresa).
ALTER TABLE empresa_fe_config
    ADD COLUMN notificar_adquirente BOOLEAN NOT NULL DEFAULT FALSE;
