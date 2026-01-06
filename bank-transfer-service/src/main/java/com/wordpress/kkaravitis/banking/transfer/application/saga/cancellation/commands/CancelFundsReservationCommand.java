package com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation.commands;


import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CancelFundsReservationCommand {
    private UUID transferId;
    private String customerId;
}
