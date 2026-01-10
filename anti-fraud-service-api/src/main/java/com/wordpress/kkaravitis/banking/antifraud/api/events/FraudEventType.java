package com.wordpress.kkaravitis.banking.antifraud.api.events;

import com.wordpress.kkaravitis.banking.common.BankingEventType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum FraudEventType implements BankingEventType {
    FRAUD_APPROVED(FraudApprovedEvent.class),

    FRAUD_REJECTED(FraudRejectedEvent.class);

    private final Class<?> payloadType;

    @Override
    public String getMessageType() {
        return this.name();
    }

    @Override
    public Class<?> getPayloadType() {
        return payloadType;
    }
}
