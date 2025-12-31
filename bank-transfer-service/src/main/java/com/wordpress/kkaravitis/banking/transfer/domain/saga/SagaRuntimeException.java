package com.wordpress.kkaravitis.banking.transfer.domain.saga;

public class SagaRuntimeException extends RuntimeException {
    public SagaRuntimeException(String message) {
        super(message);
    }

    public SagaRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
