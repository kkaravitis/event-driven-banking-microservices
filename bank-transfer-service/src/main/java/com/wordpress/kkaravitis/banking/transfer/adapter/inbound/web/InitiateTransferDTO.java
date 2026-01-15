package com.wordpress.kkaravitis.banking.transfer.adapter.inbound.web;

import java.math.BigDecimal;

public record InitiateTransferDTO(
      String fromAccountId,
      String toAccountId,
      BigDecimal amount,
      String currency
) {

}
