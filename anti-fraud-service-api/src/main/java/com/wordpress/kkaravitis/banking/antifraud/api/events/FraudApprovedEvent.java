package com.wordpress.kkaravitis.banking.antifraud.api.events;

import java.util.UUID;


public record FraudApprovedEvent(UUID transferId) {
}
