package com.wordpress.kkaravitis.banking.transfer.application.saga;

public class SagaRuntimeException extends RuntimeException {
    public SagaRuntimeException(String message) {
        super(message);
    }

    public SagaRuntimeException(Throwable cause) {
        super(cause.getMessage(),  cause);
    }

    public SagaRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
