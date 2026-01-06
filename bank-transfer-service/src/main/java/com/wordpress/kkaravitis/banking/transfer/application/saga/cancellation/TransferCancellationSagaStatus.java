package com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation;

public enum TransferCancellationSagaStatus {
    CANCEL_PENDING,
    REJECTED,
    COMPLETED,
    FAILED
}
