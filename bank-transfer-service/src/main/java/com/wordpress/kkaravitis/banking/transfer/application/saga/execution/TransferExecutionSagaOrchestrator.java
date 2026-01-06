package com.wordpress.kkaravitis.banking.transfer.application.saga.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.transfer.TransferService.InitiateTransferCommand;
import com.wordpress.kkaravitis.banking.transfer.TransferService.SagaParticipantReply;
import com.wordpress.kkaravitis.banking.transfer.application.ports.SagaStore;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransactionalOutbox.TransactionalOutboxContext;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransferStore;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEntity;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaOrchestrator;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaReplyHandlerContext;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaRuntimeException;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.commands.CheckFraudCommand;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FraudRejectedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FundsReleasedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FundsReservationFailedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.FundsReservedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.TransferApprovalFailedEvent;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.events.TransferFinalizedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.AggregateResult;
import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import com.wordpress.kkaravitis.banking.transfer.domain.Transition;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferExecutionSagaOrchestrator extends SagaOrchestrator<TransferExecutionSagaStatus, TransferExecutionSagaStepHandler> {

    public TransferExecutionSagaOrchestrator(
          SagaStore sagaStore,
          TransferStore transferStore,
          ObjectMapper objectMapper,
          List<TransferExecutionSagaStepHandler> handlers,
          TransactionalOutbox transactionalOutboxPort) {
        super(sagaStore, transferStore, objectMapper, handlers, transactionalOutboxPort);
    }

    @Transactional
    public AggregateResult start(InitiateTransferCommand command) {
        UUID transferId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();

        // create new transfer
        Transfer transfer = Transfer.createNew(
              transferId,
              command.getFromAccountId(),
              command.getToAccountId(),
              command.getAmount(),
              command.getCurrency()
        );
        transferStore.save(transfer);

        TransferExecutionSagaData sagaData = TransferExecutionSagaData.builder()
              .sagaId(sagaId)
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
              "InternalTransferSaga",
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
              .aggregateId(sagaId)
              .aggregateType("TransferExecutionSaga")
              .destinationTopic("check-fraud-commands")
              .messageType("CheckFraudCommand")
              .payload(checkFraudCommand)
              .headers(Map.of("reply-topic", "transfer-execution-saga-replies"))
              .build());

        return AggregateResult.builder()
              .transition(new Transition(null,
                    transfer.getState().name()))
              .build();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void onReply(SagaParticipantReply reply) {
        super.handleReply(SagaReplyHandlerContext.<TransferExecutionSagaStatus>builder()
              .sagaIdHeader(reply.sagaId())
              .messageType(reply.messageType())
              .payloadJson(reply.payloadJson())
              .sagaDataType(TransferExecutionSagaData.class)
              .sagaType("TransferExecutionSaga")
              .sagaReplyTopic("transfer-execution-saga-replies")
              .build());
    }

    protected SagaEvent toDomainEvent(String messageType, String payloadJson) {
        try{
            return switch (messageType) {
                case "FraudApproved" ->
                      objectMapper.readValue(payloadJson, FraudApprovedEvent.class);
                case "FraudRejected" ->
                      objectMapper.readValue(payloadJson, FraudRejectedEvent.class);
                case "FundsReserved" ->
                      objectMapper.readValue(payloadJson, FundsReservedEvent.class);
                case "FundsReservationFailed" ->
                      objectMapper.readValue(payloadJson, FundsReservationFailedEvent.class);
                case "TransferFinalized" ->
                      objectMapper.readValue(payloadJson, TransferFinalizedEvent.class);
                case "TransferApprovalFailed" ->
                      objectMapper.readValue(payloadJson, TransferApprovalFailedEvent.class);
                case "FundsReleased" ->
                      objectMapper.readValue(payloadJson, FundsReleasedEvent.class);
                default ->
                      throw new SagaRuntimeException("Unknown reply type: " + messageType);
            };
        } catch (JsonProcessingException exception) {
            throw new SagaRuntimeException(exception);
        }
    }

    @Override
    protected TransferExecutionSagaStatus getSagaFailedStatus() {
        return TransferExecutionSagaStatus.FAILED;
    }
}