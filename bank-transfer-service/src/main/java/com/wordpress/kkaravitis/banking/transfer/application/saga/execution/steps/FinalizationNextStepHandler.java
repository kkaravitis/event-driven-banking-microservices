package com.wordpress.kkaravitis.banking.transfer.application.saga.execution.steps;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaParticipantCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.commands.ReleaseFundsCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.TransferApprovalFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.TransferApprovalFailedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.TransferFinalizedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.AggregateResult;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainError;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainErrorCode;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizationNextStepHandler implements TransferExecutionSagaStepHandler {

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FINALIZATION_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferExecutionSagaStatus>> handle(
          SagaStepHandlerContext<TransferExecutionSagaStatus> context) {

        final TransferExecutionSagaData transferExecutionSagaData = (TransferExecutionSagaData) context.getSagaData();
        final SagaEvent event = context.getEvent();

        if (event instanceof TransferFinalizedEvent) {
            return completeTransfer(context);
        } else if (event instanceof TransferApprovalFailedEvent) {
            return releaseFunds(transferExecutionSagaData);
        } else if (event instanceof TransferApprovalFailedDueToCancelEvent) {
            return cancelSaga(transferExecutionSagaData);
        } else {
            return Optional.empty();
        }
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> completeTransfer(
          SagaStepHandlerContext<TransferExecutionSagaStatus> context) {

        TransferExecutionSagaStatus newSagaStatus;
        AggregateResult aggregateResult = context.getTransfer().complete();
        if (aggregateResult.isValid()) {
            newSagaStatus = TransferExecutionSagaStatus.COMPLETED;
        } else {
            DomainError domainError = aggregateResult.getError();
            if (domainError.code() == DomainErrorCode.COMPLETE_TOO_LATE) {
                newSagaStatus = TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA;
            } else {
                newSagaStatus = TransferExecutionSagaStatus.FAILED;
            }
        }

        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(context.getSagaData()
                    .withStatus(newSagaStatus))
              .build());
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> releaseFunds(
          TransferExecutionSagaData sagaData) {

        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(sagaData.withStatus(TransferExecutionSagaStatus.FUNDS_RELEASE_PENDING))
              .sagaParticipantCommand(SagaParticipantCommand.builder()
                    .messageType("ReleaseFunds")
                    .destinationTopic("accounts-service-commands")
                    .payload(ReleaseFundsCommand.builder()
                          .customerId(sagaData.getCustomerId())
                          .transferId(sagaData.getTransferId())
                          .reservationId(sagaData.getFundsReservationId())
                          .build())
                    .build())
              .build());
    }
}
