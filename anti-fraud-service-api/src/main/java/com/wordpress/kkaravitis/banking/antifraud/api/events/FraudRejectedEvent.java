package com.wordpress.kkaravitis.banking.antifraud.api.events;

import java.util.UUID;

public record FraudRejectedEvent(UUID transferId, String reason) {
}
