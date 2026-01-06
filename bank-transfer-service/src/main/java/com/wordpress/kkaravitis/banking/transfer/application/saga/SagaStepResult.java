package com.wordpress.kkaravitis.banking.transfer.application.saga;

import java.util.Optional;
import lombok.Builder;

@Builder
public class SagaStepResult<T extends Enum<T>> {
    private final SagaData<T> sagaData;
    private final SagaParticipantCommand sagaParticipantCommand;

    public SagaData<T> getSagaData() {
        return sagaData;
    }

    public Optional<SagaParticipantCommand> getSagaParticipantCommand() {
        return Optional.ofNullable(sagaParticipantCommand);
    }
}