package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events;

import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEvent;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferApprovalFailedDueToCancelEvent implements SagaEvent {
    private UUID sagaId;
    private UUID transferId;
    private String reason;
}

