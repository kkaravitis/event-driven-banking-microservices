package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution;

public enum TransferExecutionSagaStatus {
    FRAUD_CHECK_PENDING,
    FUNDS_RESERVATION_PENDING,
    FINALIZATION_PENDING,
    COMPLETED,
    COMPENSATION_PENDING,
    COMPENSATED,
    FAILED
}
