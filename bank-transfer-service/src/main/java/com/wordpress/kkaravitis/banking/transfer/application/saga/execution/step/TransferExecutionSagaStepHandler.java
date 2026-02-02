package com.wordpress.kkaravitis.banking.transfer.application.saga.execution.step;

import com.wordpress.kkaravitis.banking.account.api.events.AccountEventType;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaParticipantCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainError;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainErrorCode;
import java.util.Optional;

public interface TransferExecutionSagaStepHandler extends SagaStepHandler<TransferExecutionSagaStatus> {

    default Optional<SagaStepResult<TransferExecutionSagaStatus>> rejectTransfer(SagaStepHandlerContext<TransferExecutionSagaStatus> context) {
        DomainResult domainResult = context.getTransfer().reject();

        TransferExecutionSagaStatus newStatus;
        if (domainResult.isValid()) {
            newStatus = TransferExecutionSagaStatus.REJECTED;
        } else {
            DomainError domainError = domainResult.getError();
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

    default Optional<SagaStepResult<TransferExecutionSagaStatus>> failSagaAndSuspendTransfer(SagaStepHandlerContext<TransferExecutionSagaStatus> context, String alertsTopic) {
        context.getTransfer().suspend();
        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
                    .sagaParticipantCommand(SagaParticipantCommand.builder()
                          .messageType(AccountEventType.INCIDENT_EVENT.name())
                          .payload(context.getEvent())
                          .destinationTopic(alertsTopic)
                          .build())
              .sagaData(context.getSagaData()
                    .withStatus(TransferExecutionSagaStatus.FAILED))
              .build());
    }

}
