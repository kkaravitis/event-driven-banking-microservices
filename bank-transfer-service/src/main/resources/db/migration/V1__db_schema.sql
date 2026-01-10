CREATE TABLE IF NOT EXISTS transfer (
    id                    UUID            PRIMARY KEY,
    from_account_id       VARCHAR(255)    NOT NULL,
    to_account_id         VARCHAR(255)    NOT NULL,
    amount                NUMERIC(19, 2)  NOT NULL,
    currency              VARCHAR(8)      NOT NULL,
    state                 VARCHAR(255)    NOT NULL,
    funds_reservation_id  VARCHAR(255)    NULL,
    version               BIGINT          NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS saga (
    saga_id     UUID          PRIMARY KEY,
    saga_type   VARCHAR(255)  NOT NULL,
    saga_state  VARCHAR(255)  NOT NULL,
    saga_data   JSONB         NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);
