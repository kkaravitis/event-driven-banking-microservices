package com.wordpress.kkaravitis.banking.account.api.events;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;


public record FundsReservationFailedEvent(
      UUID transferId,
      String reason) {
}
