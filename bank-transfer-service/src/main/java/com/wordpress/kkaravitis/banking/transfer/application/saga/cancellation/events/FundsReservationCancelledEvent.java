package com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation.events;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEvent;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FundsReservationCancelledEvent implements SagaEvent {
    private UUID transferId;
}
