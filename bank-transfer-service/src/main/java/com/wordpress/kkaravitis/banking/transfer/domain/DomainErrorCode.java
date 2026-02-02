package com.wordpress.kkaravitis.banking.transfer.domain;

public enum DomainErrorCode {
    ILLEGAL_STATE,
    NOT_EXISTING,
    REJECT_TOO_LATE,
    CANCEL_TOO_LATE,
    COMPLETE_TOO_LATE,
    UNEXPECTED_TRANSITION,
    UNAUTHORIZED
}
