package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.steps;

import com.wordpress.kkaravitis.banking.transfer.TransferService;
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
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.commands.ReserveFundsCommand;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FraudRejectedEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FraudCheckedNextStepHandler implements TransferExecutionSagaStepHandler {

    private final TransferService transferService;

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FRAUD_CHECK_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferExecutionSagaStatus>> handle(SagaEvent event, SagaData<TransferExecutionSagaStatus> sagaData) {

        TransferExecutionSagaData transferExecutionSagaData = (TransferExecutionSagaData) sagaData;

        if (event instanceof FraudApprovedEvent) {
            return handleFraudApprovedEvent(transferExecutionSagaData);
        } else if (event instanceof FraudRejectedEvent) {
            return handleFraudRejectedEvent(transferExecutionSagaData);
        }

        return Optional.empty();
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFraudApprovedEvent(TransferExecutionSagaData sagaData) {
        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(sagaData.withStatus(TransferExecutionSagaStatus.FUNDS_RESERVATION_PENDING))
              .sagaParticipantCommand(SagaParticipantCommand.builder()
                    .destinationTopic("accounts-service-commands")
                    .messageType("ReserveFunds")
                    .payload(ReserveFundsCommand.builder()
                          .amount(sagaData.getAmount())
                          .currency(sagaData.getCurrency())
                          .customerId(sagaData.getCustomerId())
                          .fromAccountId(sagaData.getFromAccountId())
                          .toAccountId(sagaData.getToAccountId())
                          .transferId(sagaData.getTransferId())
                          .build())
                    .build())
              .build());
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFraudRejectedEvent(TransferExecutionSagaData sagaData) {
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
