package com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.steps;

import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.TransferCancellationSagaStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FundsReservationCancellationNextStepHandler implements SagaStepHandler<TransferCancellationSagaStatus> {
    private final TransferService transferService;

    @Override
    public TransferCancellationSagaStatus currentSagaStatus() {
        return TransferCancellationSagaStatus.ABORT_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferCancellationSagaStatus>> handle(SagaEvent event, SagaData<TransferCancellationSagaStatus> sagaData) {
        return Optional.empty();
    }
}
