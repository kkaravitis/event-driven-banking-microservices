package com.wordpress.kkaravitis.banking.transfer.application.saga;

import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

public interface SagaStepHandler<T extends Enum<T>> {

    T currentSagaStatus();

    Optional<SagaStepResult<T>> handle(SagaStepHandlerContext<T> context);

    @Getter
    @Builder
    class SagaStepHandlerContext<T extends Enum<T>> {
        private Object event;
        private SagaData<T> sagaData;
        private Transfer transfer;
    }
}
