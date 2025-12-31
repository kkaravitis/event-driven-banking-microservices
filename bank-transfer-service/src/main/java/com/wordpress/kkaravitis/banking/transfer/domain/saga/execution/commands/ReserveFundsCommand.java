package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.commands;


import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReserveFundsCommand {
    private UUID transferId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private String customerId;
}
