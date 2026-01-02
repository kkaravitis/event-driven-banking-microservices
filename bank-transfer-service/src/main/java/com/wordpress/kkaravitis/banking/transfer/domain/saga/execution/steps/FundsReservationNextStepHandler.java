package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.steps;

import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.TransferService.MarkFundsReservationCommand;
import com.wordpress.kkaravitis.banking.transfer.TransferService.RejectTransferCommand;
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
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.commands.FinalizeTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FundsReservationFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FundsReservationFailedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FundsReservedEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FundsReservationNextStepHandler implements TransferExecutionSagaStepHandler {
    private final TransferService transferService;

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FUNDS_RESERVATION_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferExecutionSagaStatus>> handle(SagaEvent event, SagaData<TransferExecutionSagaStatus> sagaData) {
        final TransferExecutionSagaData transferExecutionSagaData = (TransferExecutionSagaData) sagaData;
        return switch(event) {
            case FundsReservedEvent e ->
                  handleFundsReservedEvent(e, transferExecutionSagaData);

            case FundsReservationFailedEvent e ->
                  handleFundsReservationFailedEvent(transferExecutionSagaData);

            case FundsReservationFailedDueToCancelEvent e ->
                  handleFundsReservationFailedDueToCancelEvent(transferExecutionSagaData);

            default -> Optional.empty();
        };
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFundsReservedEvent(
          FundsReservedEvent event,
          TransferExecutionSagaData sagaData) {

        DomainResult domainResult = transferService.markFundsReservation(MarkFundsReservationCommand.builder()
                    .transferId(sagaData.getTransferId())
                    .reservationId(event.getReservationId())
              .build());

        if (domainResult.isValid()) {
            return Optional.of(SagaStepResult.
                  <TransferExecutionSagaStatus>builder()
                  .sagaData(sagaData.withReservationId(event.getReservationId())
                        .withStatus(TransferExecutionSagaStatus.FINALIZATION_PENDING))
                  .sagaParticipantCommand(SagaParticipantCommand.builder()
                        .destinationTopic("accounts-service-commands")
                        .messageType("FinalizeTransfer")
                        .payload(FinalizeTransferCommand.builder()
                              .reservationId(event.getReservationId())
                              .customerId(sagaData.getCustomerId())
                              .transferId(sagaData.getTransferId())
                              .build())
                        .build())
                  .build());
        } else {
            return Optional.of(SagaStepResult.
                  <TransferExecutionSagaStatus>builder()
                  .sagaData(sagaData
                        .withStatus(TransferExecutionSagaStatus.FAILED))
                  .build());
        }
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFundsReservationFailedEvent(TransferExecutionSagaData sagaData) {
        DomainResult domainResult = transferService.rejectTransfer(RejectTransferCommand.builder()
                    .transferId(sagaData.getTransferId())
              .build());
        TransferExecutionSagaStatus newStatus;
        if (domainResult.isValid()) {
            newStatus = TransferExecutionSagaStatus.REJECTED;
        } else {
            DomainError domainError = domainResult.getErrors().get(0);
            newStatus = domainError.code() == DomainErrorCode.REJECT_TOO_LATE ?
                  TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA : TransferExecutionSagaStatus.FAILED;
        }

        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(sagaData.withStatus(newStatus))
              .build());
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFundsReservationFailedDueToCancelEvent(TransferExecutionSagaData sagaData) {
        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(sagaData.withStatus(TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA))
              .build());
    }
}
