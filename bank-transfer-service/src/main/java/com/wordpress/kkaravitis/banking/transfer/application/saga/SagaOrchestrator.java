package com.wordpress.kkaravitis.banking.transfer.application.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.common.BankingEventType;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox.TransactionalOutboxContext;
import com.wordpress.kkaravitis.banking.transfer.application.ports.SagaStore;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransferStore;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaStepHandler.SagaStepHandlerContext;
import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class SagaOrchestrator<T extends Enum<T>, S extends SagaStepHandler<T>> {
    protected final SagaStore sagaStore;
    protected final TransferStore transferStore;
    protected final ObjectMapper objectMapper;
    protected final Map<T, SagaStepHandler<T>> handlersByStatus;
    protected final TransactionalOutbox transactionalOutbox;

    protected SagaOrchestrator(SagaStore sagaStore,
          TransferStore transferStore,
          ObjectMapper objectMapper,
          List<S> sagaStepHandlers,
          TransactionalOutbox transactionalOutbox) {
        this.sagaStore = sagaStore;
        this.transferStore = transferStore;
        this.objectMapper = objectMapper;
        this.transactionalOutbox = transactionalOutbox;

        this.handlersByStatus = sagaStepHandlers.stream().collect(Collectors.toMap(
              SagaStepHandler::currentSagaStatus,
              Function.identity(),
              (a, b) -> { throw new SagaRuntimeException("Duplicate handler for "
                    + a.currentSagaStatus()); }
        ));
    }


    protected void handleReply(SagaReplyHandlerContext<T> context) {
        UUID sagaId = sagaId(context);
        SagaEntity sagaEntity = loadSagaOrThrow(sagaId);
        SagaData<T> sagaData = parseSagaDataOrThrow(sagaEntity, context);
        T currentSagaStatus = sagaData.getStatus();
        SagaStepHandler<T> handler = handlersByStatus.get(currentSagaStatus);
        if (handler == null) {
            return;
        }
        Optional<Transfer> optionalTransfer = loadTransferOrFailSaga(sagaEntity, sagaData);
        if (optionalTransfer.isEmpty()) {
            return;
        }
        final Object event = toEvent(context);
        Transfer transfer = optionalTransfer.get();
        handler.handle(stepContext(event, sagaData, transfer))
              .ifPresent(result -> applyResult(context, sagaId, sagaEntity, transfer, result));
    }

    protected abstract List<BankingEventType> expectedBankingEventTypes();

    protected abstract T getSagaFailedStatus();

    protected String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new SagaRuntimeException("Failed to serialize JSON", e);
        }
    }

    private UUID sagaId(SagaReplyHandlerContext<T> context) {
        return UUID.fromString(context.getSagaIdHeader());
    }

    private SagaEntity loadSagaOrThrow(UUID sagaId) {
        return sagaStore.load(sagaId)
              .orElseThrow(() -> new SagaRuntimeException("Saga not found: " + sagaId));
    }

    private Optional<Transfer> loadTransferOrFailSaga(SagaEntity sagaEntity, SagaData<T> sagaData) {
        Optional<Transfer> transfer = transferStore.load(sagaData.getTransferId());
        if (transfer.isEmpty()) {
            SagaData<T> failed = sagaData.withStatus(getSagaFailedStatus());
            persistSaga(sagaEntity, failed);
        }
        return transfer;
    }

    private Object toEvent(SagaReplyHandlerContext<T> context) {
        Optional<BankingEventType> matchingEventType = expectedBankingEventTypes().stream()
              .filter(e -> e.getMessageType()
                    .equals(context.getMessageType()))
              .findFirst();
        if(matchingEventType.isEmpty()) {
            throw new SagaRuntimeException("Unexpected or Unknown reply type: " + context.getMessageType());
        }
        BankingEventType eventType = matchingEventType.get();
        try {
            return objectMapper.readValue(context.getPayloadJson(),
                  eventType.getPayloadType());
        } catch (JsonProcessingException exception) {
            throw new SagaRuntimeException(exception);
        }
    }

    private SagaData<T> parseSagaDataOrThrow(SagaEntity sagaEntity, SagaReplyHandlerContext<T> context) {
        SagaData<T> sagaData = parseSagaData(sagaEntity.getSagaDataJson(), context.getSagaDataType());
        if (sagaData == null) {
            throw new SagaRuntimeException("Saga data does not exist in saga with id " + sagaEntity.getSagaId());
        }
        return sagaData;
    }

    private void persistSaga(SagaEntity sagaEntity, SagaData<T> sagaData) {
        sagaEntity.update(sagaData.getStatus().name(), toJson(sagaData));
        sagaStore.save(sagaEntity);
    }

    private void applyResult(
          SagaReplyHandlerContext<T> context,
          UUID sagaId,
          SagaEntity sagaEntity,
          Transfer transfer,
          SagaStepResult<T> result
    ) {
        transferStore.save(transfer);
        persistSaga(sagaEntity, result.getSagaData());

        result.getSagaParticipantCommand()
              .ifPresent(cmd -> enqueueCommand(context, sagaId, cmd));
    }

    private void enqueueCommand(SagaReplyHandlerContext<T> context, UUID sagaId,
          SagaParticipantCommand command) {
        transactionalOutbox.enqueue(TransactionalOutboxContext.builder()
              .destinationTopic(command.getDestinationTopic())
              .payload(command.getPayload())
              .aggregateType(context.getSagaType())
              .aggregateId(sagaId)
              .messageType(command.getMessageType())
              .replyTopic(context.getSagaReplyTopic())
              .build());
    }

    private SagaStepHandlerContext<T> stepContext(Object event, SagaData<T> sagaData, Transfer transfer) {
        return SagaStepHandlerContext.<T>builder()
              .event(event)
              .sagaData(sagaData)
              .transfer(transfer)
              .build();
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new SagaRuntimeException(e);
        }
    }

    private SagaData<T> parseSagaData(String json, Class<? extends SagaData<T>> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new SagaRuntimeException(e);
        }
    }

}