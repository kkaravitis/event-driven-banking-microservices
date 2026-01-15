package com.wordpress.kkaravitis.banking.account.api.events;

import java.util.UUID;

public record FundsReservationCancellationRejectedEvent(UUID transferId) {
}
