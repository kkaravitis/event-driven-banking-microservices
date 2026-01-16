package com.wordpress.kkaravitis.banking.antifraud.api.commands;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CheckFraudCommand(
      UUID transferId,
      String customerId,
      String fromAccountId,
      String toAccountId,
      BigDecimal amount,
      String currency) implements Serializable {

    public CheckFraudCommand {
        Objects.requireNonNull(transferId, String.format(ERROR_TEMPLATE, "transferId"));
        Objects.requireNonNull(customerId, String.format(ERROR_TEMPLATE, "customerId"));
        Objects.requireNonNull(fromAccountId, String.format(ERROR_TEMPLATE, "fromAccountId"));
        Objects.requireNonNull(toAccountId, String.format(ERROR_TEMPLATE, "toAccountId"));
        Objects.requireNonNull(amount, String.format(ERROR_TEMPLATE, "amount"));
        Objects.requireNonNull(currency, String.format(ERROR_TEMPLATE, "currency"));
    }

    private static final String ERROR_TEMPLATE = "%s field is mandatory for fraud checking!";
    public static final String MESSAGE_TYPE = "CHECK_FRAUD";
}
