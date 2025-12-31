package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.commands;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class CheckFraudCommand {
    private UUID transferId;
    private String customerId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
}
