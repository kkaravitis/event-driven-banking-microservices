-- V1__create_account_table.sql

CREATE TABLE account (
    account_id      VARCHAR(64)   NOT NULL,
    customer_id     VARCHAR(64)   NOT NULL,
    balance         NUMERIC(19,4) NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_account PRIMARY KEY (account_id)
);

-- Optional indexes for lookups by customer
CREATE INDEX idx_account_customer_id ON account (customer_id);