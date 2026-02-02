ALTER TABLE aborted_transfer
ADD COLUMN customer_id VARCHAR(64) NOT NULL;

CREATE INDEX IF NOT EXISTS idx_aborted_transfer_transfer_customer
    ON aborted_transfer (transfer_id, customer_id);