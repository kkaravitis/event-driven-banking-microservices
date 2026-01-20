package com.wordpress.kkaravitis.banking.transfer.application.saga.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.account.api.events.AccountEventType;
import com.wordpress.kkaravitis.banking.antifraud.api.commands.CheckFraudCommand;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudEventType;
import com.wordpress.kkaravitis.banking.common.BankingEventType;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox.TransactionalOutboxContext;
import com.wordpress.kkaravitis.banking.transfer.TransferService.InitiateTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.TransferService.SagaParticipantReply;
import com.wordpress.kkaravitis.banking.transfer.application.ports.SagaStore;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransferStore;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEntity;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaOrchestrator;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaReplyHandlerContext;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.step.TransferExecutionSagaStepHandler;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import com.wordpress.kkaravitis.banking.transfer.infrastructure.kafka.Topics;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferExecutionSagaOrchestrator extends SagaOrchestrator<TransferExecutionSagaStatus, TransferExecutionSagaStepHandler> {
    private static final String TRANSFER_EXECUTION_SAGA = "TransferExecutionSaga";

    private final Topics topics;

    public TransferExecutionSagaOrchestrator(
          SagaStore sagaStore,
          TransferStore transferStore,
          ObjectMapper objectMapper,
          List<TransferExecutionSagaStepHandler> handlers,
          TransactionalOutbox transactionalOutboxPort,
          Topics topics) {
        super(sagaStore, transferStore, objectMapper, handlers, transactionalOutboxPort);
        this.topics = topics;
    }

    @Transactional
    public DomainResult start(InitiateTransferCommand command) {
        UUID transferId = UUID.randomUUID();
        Transfer transfer = Transfer.createNew(
              transferId,
              command.getFromAccountId(),
              command.getToAccountId(),
              command.getAmount(),
              command.getCurrency()
        );
        transferStore.save(transfer);

        UUID sagaId = UUID.randomUUID();
        TransferExecutionSagaData sagaData = TransferExecutionSagaData.builder()
              .transferId(transferId)
              .customerId(command.getCustomerId())
              .fromAccountId(command.getFromAccountId())
              .toAccountId(command.getToAccountId())
              .amount(command.getAmount())
              .currency(command.getCurrency())
              .status(TransferExecutionSagaStatus.FRAUD_CHECK_PENDING)
              .build();
        String sagaDataJson = writeJson(sagaData);
        SagaEntity sagaEntity = new SagaEntity(
              sagaId,
              TRANSFER_EXECUTION_SAGA,
              sagaData.getStatus().name(),
              sagaDataJson
        );
        sagaStore.save(sagaEntity);

        CheckFraudCommand checkFraudCommand = CheckFraudCommand.builder()
              .transferId(transferId)
              .customerId(command.getCustomerId())
              .fromAccountId(command.getFromAccountId())
              .toAccountId(command.getToAccountId())
              .amount(command.getAmount())
              .currency(command.getCurrency())
              .build();

        transactionalOutbox.enqueue(TransactionalOutboxContext.builder()
              .correlationId(sagaId)
              .messageType(CheckFraudCommand.MESSAGE_TYPE)
              .payload(checkFraudCommand)
              .destinationTopic(topics.antiFraudServiceCommandsTopic())
              .replyTopic(topics.transferExecutionSagaRepliesTopic())
              .build());

        return DomainResult.builder()
              .transferId(transferId)
              .build();
    }

    @Transactional
    public void onReply(SagaParticipantReply reply) {
        super.handleReply(SagaReplyHandlerContext.<TransferExecutionSagaStatus>builder()
              .sagaIdHeader(reply.sagaId())
              .messageType(reply.messageType())
              .payloadJson(reply.payloadJson())
              .sagaDataType(TransferExecutionSagaData.class)
              .sagaReplyTopic(topics.transferExecutionSagaRepliesTopic())
              .build());
    }

    @Override
    protected List<BankingEventType> expectedBankingEventTypes() {
        return List.of(
              FraudEventType.FRAUD_APPROVED,
              FraudEventType.FRAUD_REJECTED,
              AccountEventType.FUNDS_RESERVED,
              AccountEventType.FUNDS_RESERVATION_FAILED,
              AccountEventType.TRANSFER_FINALIZED,
              AccountEventType.TRANSFER_APPROVAL_FAILED,
              AccountEventType.FUNDS_RELEASED,
              AccountEventType.FUNDS_RELEASE_FAILED_DUE_TO_CANCEL,
              AccountEventType.FUNDS_RESERVATION_FAILED_DUE_TO_CANCEL,
              AccountEventType.TRANSFER_APPROVAL_FAILED_DUE_TO_CANCEL);
    }

    @Override
    protected TransferExecutionSagaStatus getSagaFailedStatus() {
        return TransferExecutionSagaStatus.FAILED;
    }
}