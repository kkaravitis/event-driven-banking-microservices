package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.commands;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FinalizeTransferCommand {
    private UUID transferId;
    private String reservationId;
    private String customerId;
}
