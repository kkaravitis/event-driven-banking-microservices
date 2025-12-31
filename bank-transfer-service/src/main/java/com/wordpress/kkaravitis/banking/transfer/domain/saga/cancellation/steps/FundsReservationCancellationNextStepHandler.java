package com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.steps;

import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.TransferCancellationSagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.TransferCancellationSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.events.FundsReservationCancellationRejectedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.events.FundsReservationCancelledEvent;

public class FundsReservationCancellationNextStepHandler implements SagaStepHandler<TransferCancellationSagaStatus> {

    @Override
    public TransferCancellationSagaStatus currentSagaStatus() {
        return TransferCancellationSagaStatus.ABORT_PENDING;
    }

    @Override
    public SagaStepResult<TransferCancellationSagaStatus> handle(SagaEvent event, SagaData<TransferCancellationSagaStatus> sagaData) {
        TransferCancellationSagaData cancellationSagaData = (TransferCancellationSagaData) sagaData;

        if (event instanceof FundsReservationCancellationRejectedEvent) {

        }

        else if (event  instanceof FundsReservationCancelledEvent) {

        }

        return null;
    }
}
