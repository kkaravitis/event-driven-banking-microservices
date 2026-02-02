package com.wordpress.kkaravitis.banking.transfer.application.saga.execution.step;

import com.wordpress.kkaravitis.banking.account.api.commands.FinalizeTransferCommand;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationFailedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.incident.AccountServiceIncidentEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaParticipantCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainError;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainErrorCode;
import com.wordpress.kkaravitis.banking.transfer.infrastructure.kafka.Topics;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FundsReservationNextStepHandler implements TransferExecutionSagaStepHandler {
    private final Topics topics;

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FUNDS_RESERVATION_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferExecutionSagaStatus>> handle(SagaStepHandlerContext<TransferExecutionSagaStatus> context) {
        if (context.getEvent() instanceof FundsReservedEvent) {
            return handleFundsReservedEvent(context);
        } else if (context.getEvent() instanceof FundsReservationFailedEvent) {
            return rejectTransfer(context);
        } else if (context.getEvent() instanceof FundsReservationFailedDueToCancelEvent) {
            return cancelSaga((TransferExecutionSagaData) context.getSagaData());
        } else if (context.getEvent() instanceof AccountServiceIncidentEvent) {
            return failSagaAndSuspendTransfer(context, topics.transferIncidentAlertsTopic());
        } else {
            return Optional.empty();
        }
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFundsReservedEvent(
          SagaStepHandlerContext<TransferExecutionSagaStatus> context) {

        final FundsReservedEvent event = (FundsReservedEvent)context.getEvent();
        final TransferExecutionSagaData sagaData = (TransferExecutionSagaData) context.getSagaData();

        DomainResult domainResult = context.getTransfer().startCompletion(event.reservationId());

        if (domainResult.isValid()) {
            return Optional.of(SagaStepResult.
                  <TransferExecutionSagaStatus>builder()
                  .sagaData(sagaData.withReservationId(event.reservationId())
                        .withStatus(TransferExecutionSagaStatus.FINALIZATION_PENDING))
                  .sagaParticipantCommand(SagaParticipantCommand.builder()
                        .destinationTopic(topics.accountsServiceCommandsTopic())
                        .messageType(FinalizeTransferCommand.MESSAGE_TYPE)
                        .payload(FinalizeTransferCommand.builder()
                              .reservationId(event.reservationId())
                              .customerId(sagaData.getCustomerId())
                              .transferId(sagaData.getTransferId())
                              .build())
                        .build())
                  .build());
        } else {
            TransferExecutionSagaStatus newSagaStatus;
            DomainError domainError = domainResult.getError();
            if (domainError.code() == DomainErrorCode.COMPLETE_TOO_LATE) {
                newSagaStatus = TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA;
            } else {
                newSagaStatus = TransferExecutionSagaStatus.FAILED;
            }
            return Optional.of(SagaStepResult.
                  <TransferExecutionSagaStatus>builder()
                  .sagaData(sagaData
                        .withStatus(newSagaStatus))
                  .build());
        }
    }
}
