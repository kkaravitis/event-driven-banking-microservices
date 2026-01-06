package com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation.steps;

import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation.TransferCancellationSagaData;
import com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation.TransferCancellationSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation.TransferCancellationSagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation.events.FundsReservationCancellationRejectedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation.events.FundsReservationCancelledEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.AggregateResult;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FundsReservationCancellationNextStepHandler implements TransferCancellationSagaStepHandler {

    @Override
    public TransferCancellationSagaStatus currentSagaStatus() {
        return TransferCancellationSagaStatus.CANCEL_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferCancellationSagaStatus>> handle(SagaStepHandlerContext<TransferCancellationSagaStatus> context) {
        TransferCancellationSagaData transferCancellationSagaData = (TransferCancellationSagaData) context.getSagaData();
        if (context.getEvent() instanceof FundsReservationCancelledEvent) {
            AggregateResult aggregateResult = context.getTransfer().markCancelled();
            TransferCancellationSagaStatus newStatus;
            if (aggregateResult.isValid()) {
                newStatus = TransferCancellationSagaStatus.COMPLETED;
            } else if (aggregateResult.getError().code() == DomainErrorCode.CANCEL_TOO_LATE) {
                newStatus = TransferCancellationSagaStatus.REJECTED;
            } else {
                newStatus = TransferCancellationSagaStatus.FAILED;
            }
            return Optional.of(SagaStepResult.<TransferCancellationSagaStatus>builder()
                        .sagaData(transferCancellationSagaData.withStatus(newStatus))
                  .build());
        } else if (context.getEvent() instanceof FundsReservationCancellationRejectedEvent) {
            return Optional.of(SagaStepResult.<TransferCancellationSagaStatus>builder()
                  .sagaData(transferCancellationSagaData.withStatus(TransferCancellationSagaStatus.REJECTED))
                  .build());
        }
        return Optional.empty();
    }
}
