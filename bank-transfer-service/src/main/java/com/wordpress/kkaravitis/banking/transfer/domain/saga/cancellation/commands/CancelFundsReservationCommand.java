package com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.commands;


import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CancelFundsReservationCommand {
    private UUID transferId;
}
