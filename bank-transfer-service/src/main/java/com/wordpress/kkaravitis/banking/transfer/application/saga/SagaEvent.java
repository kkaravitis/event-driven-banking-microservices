package com.wordpress.kkaravitis.banking.transfer.application.saga;

import java.util.UUID;

public interface SagaEvent {
    UUID getTransferId();
}
