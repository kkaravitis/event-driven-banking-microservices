package com.wordpress.kkaravitis.banking.account.api.events;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FundsReservationFailedDueToCancelEvent {
    private UUID transferId;
    private String reason;
}
