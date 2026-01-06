package com.wordpress.kkaravitis.banking.transfer.application.saga.execution;

public enum TransferExecutionSagaStatus {
    FRAUD_CHECK_PENDING,
    FUNDS_RESERVATION_PENDING,
    FINALIZATION_PENDING,
    COMPLETED,
    FUNDS_RELEASE_PENDING,
    REJECTED,
    CANCELLED_BY_CANCEL_SAGA,
    FAILED
}
