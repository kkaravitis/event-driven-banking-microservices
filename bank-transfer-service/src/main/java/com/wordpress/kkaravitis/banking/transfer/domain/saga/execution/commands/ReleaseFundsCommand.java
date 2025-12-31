package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.commands;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReleaseFundsCommand {
    private UUID transferId;
    private String reservationId;
    private BigDecimal amount;
    private String fromAccountId;
    private String customerId;
}
