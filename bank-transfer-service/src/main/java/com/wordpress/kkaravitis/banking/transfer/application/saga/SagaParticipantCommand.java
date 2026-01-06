package com.wordpress.kkaravitis.banking.transfer.application.saga;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public
class SagaParticipantCommand {
    private final String destinationTopic;
    private final String messageType;
    private final Object payload;
}