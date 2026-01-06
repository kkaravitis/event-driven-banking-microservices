package com.wordpress.kkaravitis.banking.transfer.application.saga.execution.commands;

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
