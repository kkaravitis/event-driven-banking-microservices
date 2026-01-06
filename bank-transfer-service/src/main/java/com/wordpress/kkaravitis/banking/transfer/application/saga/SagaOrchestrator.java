package com.wordpress.kkaravitis.banking.transfer.application.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.transfer.application.ports.SagaStore;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransactionalOutbox.TransactionalOutboxContext;
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

        SagaStepHandler<T> handler = handlersByStatus.get(sagaData.getStatus());
        if (handler == null) {
            return;
        }

        final SagaEvent event = toEvent(context);

        validateTransferId(event, sagaData);

        Transfer transfer = loadTransferOrFailSaga(sagaEntity, sagaData).orElse(null);
        if (transfer == null) {
            return;
        }

        handler.handle(stepContext(event, sagaData, transfer))
              .ifPresent(result -> applyResult(context, sagaId, sagaEntity, transfer, result));
    }

    protected abstract SagaEvent toDomainEvent(String messageType, String payloadJson);

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

    private void validateTransferId(SagaEvent event, SagaData<T> sagaData) {
        UUID expected = sagaData.getTransferId();
        if (event.getTransferId() == null || !event.getTransferId().equals(expected)) {
            throw new SagaRuntimeException("Invalid transferId inside saga event. Transfer id should be " + expected);
        }
    }

    private SagaEvent toEvent(SagaReplyHandlerContext<T> context) {
        return toDomainEvent(context.getMessageType(), context.getPayloadJson());
    }

    private SagaData<T> parseSagaDataOrThrow(SagaEntity sagaEntity, SagaReplyHandlerContext<T> context) {
        SagaData<T> sagaData = parseSagaData(sagaEntity.getSagaDataJson(), context.getSagaDataType());
        if (sagaData == null) {
            throw new SagaRuntimeException("Saga data does not exist in saga with id " + sagaEntity.getSagaId());
        }
        return sagaData;
    }

    private void persistSaga(SagaEntity sagaEntity, SagaData<T> sagaData) {
        sagaEntity.setSagaState(sagaData.getStatus().name());
        sagaEntity.setSagaDataJson(toJson(sagaData));
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

    private void enqueueCommand(SagaReplyHandlerContext<T> context, UUID sagaId, SagaParticipantCommand cmd) {
        transactionalOutbox.enqueue(TransactionalOutboxContext.builder()
              .aggregateType(context.getSagaType())
              .messageType(cmd.getMessageType())
              .destinationTopic(cmd.getDestinationTopic())
              .aggregateId(sagaId)
              .headers(Map.of("reply-topic", context.getSagaReplyTopic()))
              .payload(cmd.getPayload())
              .build());
    }

    private SagaStepHandlerContext<T> stepContext(SagaEvent event, SagaData<T> sagaData, Transfer transfer) {
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