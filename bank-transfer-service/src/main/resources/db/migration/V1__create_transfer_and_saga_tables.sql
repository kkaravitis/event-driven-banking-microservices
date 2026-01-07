CREATE TABLE IF NOT EXISTS transfer (
    id                    uuid            PRIMARY KEY,
    from_account_id       text            NOT NULL,
    to_account_id         text            NOT NULL,
    amount                numeric(19, 2)  NOT NULL,
    currency              text            NOT NULL,
    state                 text            NOT NULL,
    funds_reservation_id  text            NULL,
    version               bigint          NOT NULL DEFAULT 0,
    created_at            timestamptz     NOT NULL DEFAULT now(),
    updated_at            timestamptz     NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS saga (
    saga_id     uuid         PRIMARY KEY,
    saga_type   text         NOT NULL,
    saga_state  text         NOT NULL,
    saga_data   jsonb        NOT NULL,
    version     bigint       NOT NULL DEFAULT 0,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
