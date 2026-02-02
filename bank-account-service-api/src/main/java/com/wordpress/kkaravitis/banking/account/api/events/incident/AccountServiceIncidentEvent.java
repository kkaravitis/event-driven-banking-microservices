package com.wordpress.kkaravitis.banking.account.api.events.incident;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AccountServiceIncidentEvent(
      UUID transferId,
      AccountServiceIncidentAction action,
      AccountServiceIncidentReason reason,
      String reservationId,
      String fromAccountId,
      String toAccountId,
      String customerId,
      Instant occurredAt,
      String message
) {}
