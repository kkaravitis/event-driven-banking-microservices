package com.wordpress.kkaravitis.banking.account.api.events.incident;

public enum AccountServiceIncidentReason {
    UNAUTHORIZED,
    RESERVATION_NOT_FOUND,
    FROM_ACCOUNT_NOT_FOUND,
    TO_ACCOUNT_NOT_FOUND,
    RESERVATION_ACCOUNT_MISMATCH,
    RESERVATION_TRANSFER_MISMATCH,
    INVALID_STATE
}
