package com.wordpress.kkaravitis.banking.account.api.events;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FundsReservationFailedEvent {
    private UUID transferId;
    private String reason;
}
