CREATE TABLE blacklisted_account (
    account_id  VARCHAR(64)  PRIMARY KEY,
    reason      TEXT         NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
);

CREATE TABLE outbox_message (
    message_id         VARCHAR(255)   PRIMARY KEY,
    correlation_id     UUID           NOT NULL,
    message_type       VARCHAR(255)   NOT NULL,
    payload            JSONB          NOT NULL,
    destination_topic  VARCHAR(255)   NOT NULL,
    created_at         TIMESTAMPZ     NOT NULL DEFAULT NOW()
);