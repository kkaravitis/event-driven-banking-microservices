package com.wordpress.kkaravitis.banking.account.api.events;

import java.util.UUID;


public record FundsReservationFailedEvent(
      UUID transferId,
      String reason) {
}
