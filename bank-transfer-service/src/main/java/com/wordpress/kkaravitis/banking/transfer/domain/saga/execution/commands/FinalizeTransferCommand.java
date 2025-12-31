package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.commands;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FinalizeTransferCommand {
    private UUID transferId;
    private String reservationId;
    private String customerId;
}
