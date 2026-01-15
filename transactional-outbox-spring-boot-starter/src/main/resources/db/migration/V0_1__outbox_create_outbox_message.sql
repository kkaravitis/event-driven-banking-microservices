-- Outbox table for the transactional outbox pattern (PostgreSQL)

CREATE TABLE IF NOT EXISTS outbox_message (
    message_id         UUID          PRIMARY KEY,
    destination_topic  VARCHAR(255)  NOT NULL,
    payload            TEXT         NOT NULL,
    correlation_id     UUID          NOT NULL,
    message_type       VARCHAR(255)  NOT NULL,
    reply_topic        VARCHAR(255)  NULL,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_outbox_message_created_at
    ON outbox_message (created_at);
