package com.wordpress.kkaravitis.banking.transfer.domain.saga;

public interface SagaData<T extends Enum<T>> {
    T getStatus();
}
