package com.wordpress.kkaravitis.banking.account.api.events;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record TransferApprovalFailedDueToCancelEvent (
    UUID transferId,
    String reason
){}

