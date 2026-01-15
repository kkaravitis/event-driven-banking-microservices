package com.wordpress.kkaravitis.banking.antifraud.api.commands;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record CheckFraudCommand(
      UUID transferId,
      String customerId,
      String fromAccountId,
      String toAccountId,
      BigDecimal amount,
      String currency) implements Serializable {

    public static final String MESSAGE_TYPE = "CHECK_FRAUD";
}
