package com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.events;

import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEvent;
import java.util.UUID;

public class FundsReservationCancelledEvent implements SagaEvent {
    private UUID transferId;
}
