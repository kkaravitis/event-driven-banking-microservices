package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.steps;

import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.TransferService.CompleteTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainError;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainErrorCode;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaParticipantCommand;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.commands.ReleaseFundsCommand;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.TransferApprovalFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.TransferApprovalFailedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.TransferFinalizedEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FinalizationNextStepHandler implements TransferExecutionSagaStepHandler {

    private final TransferService transferService;

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FINALIZATION_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferExecutionSagaStatus>> handle(SagaEvent event, SagaData<TransferExecutionSagaStatus> sagaData) {

        final TransferExecutionSagaData transferExecutionSagaData = (TransferExecutionSagaData) sagaData;

        return switch(event) {
            case TransferFinalizedEvent e ->
                  handleTransferFinalizedEvent(transferExecutionSagaData);

            case TransferApprovalFailedEvent e ->
                  handleTransferApprovalFailedEvent(transferExecutionSagaData);

            case TransferApprovalFailedDueToCancelEvent e ->
                  handleTransferApprovalFailedDueToCancelEvent(transferExecutionSagaData);

            default -> Optional.empty();
        };
    }

    Optional<SagaStepResult<TransferExecutionSagaStatus>> handleTransferFinalizedEvent(TransferExecutionSagaData sagaData) {
        DomainResult domainResult = transferService.completeTransfer(CompleteTransferCommand.builder()
                    .transferId(sagaData.getTransferId())
              .build());

        TransferExecutionSagaStatus newSagaStatus;
        if (domainResult.isValid()) {
            newSagaStatus = TransferExecutionSagaStatus.COMPLETED;
        } else {
            DomainError domainError = domainResult.getErrors().get(0);
            if (domainError.code() == DomainErrorCode.COMPLETE_TOO_LATE) {
                newSagaStatus = TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA;
            } else {
                newSagaStatus = TransferExecutionSagaStatus.FAILED;
            }
        }

        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
                    .sagaData(sagaData.withStatus(newSagaStatus))
              .build());

    }

    Optional<SagaStepResult<TransferExecutionSagaStatus>> handleTransferApprovalFailedEvent(TransferExecutionSagaData sagaData) {
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

    Optional<SagaStepResult<TransferExecutionSagaStatus>> handleTransferApprovalFailedDueToCancelEvent(TransferExecutionSagaData sagaData) {
        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(sagaData.withStatus(TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA))
              .build());
    }
}
