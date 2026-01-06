package com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEvent;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransferApprovalFailedDueToCancelEvent implements SagaEvent {
    private UUID transferId;
    private String reason;
}

