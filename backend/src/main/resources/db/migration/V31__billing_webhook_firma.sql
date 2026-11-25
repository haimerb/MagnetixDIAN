ALTER TABLE billing_webhook_event ADD COLUMN IF NOT EXISTS firmado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_billing_webhook_event_referencia ON billing_webhook_event(referencia);
