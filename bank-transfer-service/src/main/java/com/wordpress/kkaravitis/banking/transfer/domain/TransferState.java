package com.wordpress.kkaravitis.banking.transfer.domain;

public enum TransferState {
    REQUESTED,
    COMPLETION_PENDING,
    COMPLETED,
    REJECTED,
    CANCEL_PENDING,
    CANCELLED
}
