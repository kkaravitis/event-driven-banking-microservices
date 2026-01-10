package com.wordpress.kkaravitis.banking.transfer.application.saga.execution;

import com.wordpress.kkaravitis.banking.account.api.commands.ReserveFundsCommand;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudRejectedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaParticipantCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepResult;
import com.wordpress.kkaravitis.banking.transfer.infrastructure.kafka.Topics;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FraudCheckedNextStepHandler implements TransferExecutionSagaStepHandler {

    private final Topics topics;

    @Override
    public TransferExecutionSagaStatus currentSagaStatus() {
        return TransferExecutionSagaStatus.FRAUD_CHECK_PENDING;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Optional<SagaStepResult<TransferExecutionSagaStatus>> handle(SagaStepHandlerContext<TransferExecutionSagaStatus> context) {

        TransferExecutionSagaData transferExecutionSagaData = (TransferExecutionSagaData) context.getSagaData();

        if (context.getEvent() instanceof FraudApprovedEvent) {
            return handleFraudApprovedEvent(transferExecutionSagaData);
        } else if (context.getEvent() instanceof FraudRejectedEvent) {
            return rejectTransfer(context);
        }

        return Optional.empty();
    }

    private Optional<SagaStepResult<TransferExecutionSagaStatus>> handleFraudApprovedEvent(TransferExecutionSagaData sagaData) {
        return Optional.of(SagaStepResult.<TransferExecutionSagaStatus>builder()
              .sagaData(sagaData.withStatus(TransferExecutionSagaStatus.FUNDS_RESERVATION_PENDING))
              .sagaParticipantCommand(SagaParticipantCommand.builder()
                    .destinationTopic(topics.accountsServiceCommandsTopic())
                    .messageType(ReserveFundsCommand.MESSAGE_TYPE)
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
}
