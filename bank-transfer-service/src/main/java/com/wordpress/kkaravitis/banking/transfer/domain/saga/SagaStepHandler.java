package com.wordpress.kkaravitis.banking.transfer.domain.saga;

import java.util.Optional;

public interface SagaStepHandler<T extends Enum<T>> {

    T currentSagaStatus();

    Optional<SagaStepResult<T>> handle(SagaEvent event, SagaData<T> sagaData);
}
