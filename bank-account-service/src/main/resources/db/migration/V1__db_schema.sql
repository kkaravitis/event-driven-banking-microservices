
CREATE TABLE account (
    account_id          VARCHAR(64)   NOT NULL,
    customer_id         VARCHAR(64)   NOT NULL,
    available_balance   NUMERIC(19,4) NOT NULL DEFAULT 0,
    reserved_balance    NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency            VARCHAR(3)    NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_account PRIMARY KEY (account_id)
);
CREATE INDEX idx_account_customer_id ON account (customer_id);

CREATE TABLE funds_reservation (
    reservation_id      VARCHAR(64)   NOT NULL,
    transfer_id         UUID          NOT NULL,
    from_account_id     VARCHAR(64)   NOT NULL,
    to_account_id       VARCHAR(64)   NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3)    NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    release_reason      VARCHAR(32),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version             BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_funds_reservation PRIMARY KEY (reservation_id),

    CONSTRAINT fk_funds_reservation_from_account
        FOREIGN KEY (from_account_id)
        REFERENCES account (account_id),

    CONSTRAINT fk_funds_reservation_to_account
        FOREIGN KEY (to_account_id)
        REFERENCES account (account_id)
);

CREATE UNIQUE INDEX ux_funds_reservation_transfer_id
    ON funds_reservation (transfer_id);

CREATE INDEX idx_funds_reservation_from_account
    ON funds_reservation (from_account_id);

CREATE INDEX idx_funds_reservation_to_account
    ON funds_reservation (to_account_id);

CREATE INDEX idx_funds_reservation_status
    ON funds_reservation (status);

CREATE TABLE aborted_transfer (
    transfer_id  UUID       NOT NULL,
    aborted_at   TIMESTAMP  NOT NULL,
    reason       TEXT       NULL,

    CONSTRAINT pk_aborted_transfer PRIMARY KEY (transfer_id)
);

INSERT INTO account (
account_id, customer_id, available_balance, reserved_balance, currency)
VALUES
('ACC-201', 'CC-200', 10000, 0, 'EUR');

INSERT INTO account (
account_id, customer_id, available_balance, reserved_balance, currency)
VALUES
('ACC-101', 'CC-201', 30000, 0, 'EUR');
