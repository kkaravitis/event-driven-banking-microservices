package com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEvent;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FraudApprovedEvent implements SagaEvent {
    private UUID transferId;
}
