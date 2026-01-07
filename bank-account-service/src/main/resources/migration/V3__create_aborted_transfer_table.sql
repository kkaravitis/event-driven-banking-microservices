-- V3__create_aborted_transfer_table.sql

CREATE TABLE aborted_transfer (
    transfer_id  UUID       NOT NULL,
    aborted_at   TIMESTAMP  NOT NULL,
    reason       TEXT       NULL,

    CONSTRAINT pk_aborted_transfer PRIMARY KEY (transfer_id)
);
