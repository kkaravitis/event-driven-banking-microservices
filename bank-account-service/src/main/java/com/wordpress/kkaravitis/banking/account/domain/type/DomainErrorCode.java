package com.wordpress.kkaravitis.banking.account.domain.type;

public enum DomainErrorCode {
    INVALID_AMOUNT,
    CURRENCY_MISMATCH,
    INSUFFICIENT_AVAILABLE_FUNDS,
    INSUFFICIENT_RESERVED_FUNDS,
    RESERVATION_RELEASED,
    RESERVATION_FINALIZED,
    INVALID_ACCOUNT
}
