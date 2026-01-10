package com.wordpress.kkaravitis.banking.antifraud.api.commands;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckFraudCommand {
    public static final String MESSAGE_TYPE = "CHECK_FRAUD";

    private UUID transferId;
    private String customerId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
}
