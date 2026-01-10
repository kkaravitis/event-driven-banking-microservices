package com.wordpress.kkaravitis.banking.transfer.application.saga;

import java.util.UUID;

public interface SagaData<T extends Enum<T>> {
    UUID getTransferId();

    T getStatus();

    SagaData<T> withStatus(T newStatus);
}
