CREATE TABLE IF NOT EXISTS transfer (
    id                    UUID            PRIMARY KEY,
    from_account_id       VARCHAR(255)    NOT NULL,
    to_account_id         VARCHAR(255)    NOT NULL,
    amount                NUMERIC(19, 2)  NOT NULL,
    currency              VARCHAR(8)      NOT NULL,
    state                 VARCHAR(255)    NOT NULL,
    funds_reservation_id  VARCHAR(255)    NULL,
    version               BIGINT          NOT NULL DEFAULT 0,
    created_at            TIMESTAMPZ      NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPZ      NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS saga (
    saga_id     UUID          PRIMARY KEY,
    saga_type   VARCHAR(255)  NOT NULL,
    saga_state  VARCHAR(255)  NOT NULL,
    saga_data   JSONB         NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMPZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPZ    NOT NULL DEFAULT now()
);

CREATE TABLE outbox_message (
    message_id         VARCHAR(255)   PRIMARY KEY,
    destination_topic  VARCHAR(255)   NOT NULL,
    payload            JSONB          NOT NULL,
    aggregate_type     VARCHAR(255)   PRIMARY KEY,
    aggregate_id       UUID           NOT NULL,
    message_type       VARCHAR(255)   NOT NULL,
    reply_topic        VARCHAR(255)   NULL,
    created_at         TIMESTAMPZ     NOT NULL DEFAULT NOW()
);
