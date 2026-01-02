package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.steps;

import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.TransferService.RejectTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainError;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainErrorCode;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FundsReleaseFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FundsReleasedEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FundsReleaseNextStepHandler implements TransferExecutionSagaStepHandler {

    private final TransferService transferService;

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FUNDS_RELEASE_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferExecutionSagaStatus>> handle(SagaEvent event, SagaData<TransferExecutionSagaStatus> sagaData) {
        TransferExecutionSagaData transferExecutionSagaData = (TransferExecutionSagaData) sagaData;

        if (event instanceof FundsReleasedEvent) {
            return handleFundsReleasedEvent(transferExecutionSagaData);
        } else if (event instanceof FundsReleaseFailedDueToCancelEvent) {
            return handleFundsReleaseFailedDueToCancelEvent(transferExecutionSagaData);
        }

        return Optional.empty();
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFundsReleaseFailedDueToCancelEvent(TransferExecutionSagaData sagaData) {
        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(sagaData.withStatus(TransferExecutionSagaStatus.CANCELLED_BY_CANCEL_SAGA))
              .build());
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFundsReleasedEvent(TransferExecutionSagaData sagaData) {
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
}
