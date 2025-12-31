package com.wordpress.kkaravitis.banking.transfer.domain;

public enum TransferState {
    PENDING,
    COMPLETED,
    REJECTED,
    CANCEL_PENDING,
    CANCELLED
}
