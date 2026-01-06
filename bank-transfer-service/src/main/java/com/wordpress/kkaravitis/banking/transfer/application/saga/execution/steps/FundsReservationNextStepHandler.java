package com.wordpress.kkaravitis.banking.transfer.application.saga.execution.steps;

import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaParticipantCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.commands.FinalizeTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FundsReservationFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FundsReservationFailedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FundsReservedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.AggregateResult;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FundsReservationNextStepHandler implements TransferExecutionSagaStepHandler {

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
        } else {
            return Optional.empty();
        }
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFundsReservedEvent(
          SagaStepHandlerContext<TransferExecutionSagaStatus> context) {

        final FundsReservedEvent event = (FundsReservedEvent)context.getEvent();
        final TransferExecutionSagaData sagaData = (TransferExecutionSagaData) context.getSagaData();

        AggregateResult aggregateResult = context.getTransfer().notifyFundsReservation(event.getReservationId());

        if (aggregateResult.isValid()) {
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
}
