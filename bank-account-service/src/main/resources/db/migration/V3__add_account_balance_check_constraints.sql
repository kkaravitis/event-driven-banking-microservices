ALTER TABLE account
    ADD CONSTRAINT chk_account_available_balance_nonnegative
        CHECK (available_balance >= 0);

ALTER TABLE account
    ADD CONSTRAINT chk_account_reserved_balance_nonnegative
        CHECK (reserved_balance >= 0);