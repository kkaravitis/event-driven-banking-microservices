CREATE TABLE blacklisted_account (
    account_id  VARCHAR(64)  PRIMARY KEY,
    reason      TEXT         NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
);