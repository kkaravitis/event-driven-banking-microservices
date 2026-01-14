package com.wordpress.kkaravitis.banking.transfer.application.saga.execution;

import com.wordpress.kkaravitis.banking.account.api.commands.ReleaseFundsCommand;
import com.wordpress.kkaravitis.banking.account.api.events.TransferApprovalFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.account.api.events.TransferApprovalFailedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.TransferFinalizedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaParticipantCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainError;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainErrorCode;
import com.wordpress.kkaravitis.banking.transfer.infrastructure.kafka.Topics;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FinalizationNextStepHandler implements TransferExecutionSagaStepHandler {

    private final Topics topics;

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FINALIZATION_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferExecutionSagaStatus>> handle(
          SagaStepHandlerContext<TransferExecutionSagaStatus> context) {

        final TransferExecutionSagaData transferExecutionSagaData = (TransferExecutionSagaData) context.getSagaData();
        final Object event = context.getEvent();

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
        DomainResult aggregateResult = context.getTransfer().markCompleted();
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
                    .destinationTopic(topics.accountsServiceCommandsTopic())
                    .messageType(ReleaseFundsCommand.MESSAGE_TYPE)
                    .payload(ReleaseFundsCommand.builder()
                          .customerId(sagaData.getCustomerId())
                          .transferId(sagaData.getTransferId())
                          .reservationId(sagaData.getFundsReservationId())
                          .build())
                    .build())
              .build());
    }
}
