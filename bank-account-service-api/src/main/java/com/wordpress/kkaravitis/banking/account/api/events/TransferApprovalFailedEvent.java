package com.wordpress.kkaravitis.banking.account.api.events;

import java.util.UUID;

public record TransferApprovalFailedEvent(
      UUID transferId,
      String reason) {
}

