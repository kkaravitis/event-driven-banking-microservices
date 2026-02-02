package com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation;

import com.wordpress.kkaravitis.banking.account.api.events.AccountEventType;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationCancelledEvent;
import com.wordpress.kkaravitis.banking.account.api.events.incident.AccountServiceIncidentEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaParticipantCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.infrastructure.kafka.Topics;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FundsReservationCancellationNextStepHandler implements TransferCancellationSagaStepHandler {

    private final Topics topics;

    @Override
    public TransferCancellationSagaStatus currentSagaStatus() {
        return TransferCancellationSagaStatus.CANCEL_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferCancellationSagaStatus>> handle(SagaStepHandlerContext<TransferCancellationSagaStatus> context) {
        TransferCancellationSagaData transferCancellationSagaData = (TransferCancellationSagaData) context.getSagaData();
        if (context.getEvent() instanceof FundsReservationCancelledEvent) {
            DomainResult domainResult = context.getTransfer().markCancelled();
            TransferCancellationSagaStatus newStatus;
            if (domainResult.isValid()) {
                newStatus = TransferCancellationSagaStatus.COMPLETED;
            } else {
                newStatus = TransferCancellationSagaStatus.FAILED;
            }
            return Optional.of(SagaStepResult.<TransferCancellationSagaStatus>builder()
                        .sagaData(transferCancellationSagaData.withStatus(newStatus))
                  .build());
        } else if (context.getEvent() instanceof AccountServiceIncidentEvent) {
            context.getTransfer().suspend();
            return Optional.of(SagaStepResult.<TransferCancellationSagaStatus>builder()
                  .sagaParticipantCommand(SagaParticipantCommand.builder()
                        .messageType(AccountEventType.INCIDENT_EVENT.name())
                        .payload(context.getEvent())
                        .destinationTopic(topics.transferIncidentAlertsTopic())
                        .build())
                  .sagaData(transferCancellationSagaData
                        .withStatus(TransferCancellationSagaStatus.FAILED))
                  .build());
        }
        return Optional.empty();
    }
}
