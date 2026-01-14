package com.wordpress.kkaravitis.banking.antifraud.application;

import lombok.Builder;
import lombok.Getter;

public interface MessageBrokerPort {

    void send(OutboundMessage outboundMessage);

    @Builder
    @Getter
    class OutboundMessage {
        private final String destinationTopic;
        private final String messageId;
        private final String correlationId;
        private final String messageType;
        private final Object payload;
    }
}
