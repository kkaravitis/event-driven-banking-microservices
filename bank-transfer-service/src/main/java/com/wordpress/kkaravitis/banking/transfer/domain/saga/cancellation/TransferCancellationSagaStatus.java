package com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation;

public enum TransferCancellationSagaStatus {
    ABORT_PENDING,
    REJECTED,
    COMPLETED,
    FAILED
}
