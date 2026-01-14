package com.wordpress.kkaravitis.banking.transfer.application.saga.execution;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainError;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainErrorCode;
import java.util.Optional;

public interface TransferExecutionSagaStepHandler extends SagaStepHandler<TransferExecutionSagaStatus> {

    default Optional<SagaStepResult<TransferExecutionSagaStatus>> rejectTransfer(SagaStepHandlerContext<TransferExecutionSagaStatus> context) {
        DomainResult aggregateResult = context.getTransfer().reject();

        TransferExecutionSagaStatus newStatus;
        if (aggregateResult.isValid()) {
            newStatus = TransferExecutionSagaStatus.REJECTED;
        } else {
            DomainError domainError = aggregateResult.getError();
            newStatus = domainError.code() == DomainErrorCode.REJECT_TOO_LATE ?
                  TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA : TransferExecutionSagaStatus.FAILED;
        }

        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(context.getSagaData().withStatus(newStatus))
              .build());
    }

    default Optional<SagaStepResult<TransferExecutionSagaStatus>> cancelSaga(TransferExecutionSagaData sagaData) {
        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(sagaData.withStatus(TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA))
              .build());
    }

}
