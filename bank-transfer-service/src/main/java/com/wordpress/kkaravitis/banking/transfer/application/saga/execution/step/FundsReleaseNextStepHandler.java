package com.wordpress.kkaravitis.banking.transfer.application.saga.execution.step;

import com.wordpress.kkaravitis.banking.account.api.events.FundsReleaseFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReleasedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FundsReleaseNextStepHandler implements TransferExecutionSagaStepHandler {

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FUNDS_RELEASE_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferExecutionSagaStatus>> handle(SagaStepHandlerContext<TransferExecutionSagaStatus> context) {

        if (context.getEvent() instanceof FundsReleasedEvent) {
            return rejectTransfer(context);
        } else if (context.getEvent() instanceof FundsReleaseFailedDueToCancelEvent) {
            return cancelSaga((TransferExecutionSagaData) context.getSagaData());
        } else {
            return Optional.empty();
        }
    }

}
