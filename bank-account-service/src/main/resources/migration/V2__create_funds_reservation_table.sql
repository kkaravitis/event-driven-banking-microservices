-- V2__create_funds_reservation_table.sql

CREATE TABLE funds_reservation (
    reservation_id      VARCHAR(64)   NOT NULL,
    transfer_id         UUID          NOT NULL,
    from_account_id     VARCHAR(64)   NOT NULL,
    to_account_id       VARCHAR(64)   NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3)    NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_funds_reservation PRIMARY KEY (reservation_id),

    CONSTRAINT fk_funds_reservation_from_account
        FOREIGN KEY (from_account_id)
        REFERENCES account (account_id),

    CONSTRAINT fk_funds_reservation_to_account
        FOREIGN KEY (to_account_id)
        REFERENCES account (account_id)
);

-- Each transfer should have at most one reservation row
CREATE UNIQUE INDEX ux_funds_reservation_transfer_id
    ON funds_reservation (transfer_id);

-- Optional indexes for reporting / access patterns
CREATE INDEX idx_funds_reservation_from_account
    ON funds_reservation (from_account_id);

CREATE INDEX idx_funds_reservation_to_account
    ON funds_reservation (to_account_id);

CREATE INDEX idx_funds_reservation_status
    ON funds_reservation (status);
