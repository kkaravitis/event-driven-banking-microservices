ALTER TABLE transfer
    ADD COLUMN customer_id VARCHAR(64) NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transfer_customer_id
    ON transfer (customer_id);

CREATE INDEX IF NOT EXISTS idx_transfer_id_customer_id
    ON transfer (id, customer_id);