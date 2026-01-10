package com.wordpress.kkaravitis.banking.account.api.events;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransferApprovalFailedEvent {
    private UUID transferId;
    private String reason;
}

