package com.wordpress.kkaravitis.banking.transfer.domain.saga;

import lombok.Builder;
import lombok.Getter;

public interface SagaStepHandler<T extends Enum<T>> {

    T currentSagaStatus();

    SagaStepResult<T> handle(SagaEvent event, SagaData<T> sagaData);

    @Builder
    @Getter
    class SagaStepResult<T extends Enum<T>> {
        private final SagaData<T> sagaData;
        private final SagaParticipantCommand sagaParticipantCommand;
    }

    @Builder
    @Getter
    class SagaParticipantCommand {
        private final String destinationTopic;
        private final String messageType;
        private final Object payload;
    }

}
