package com.wordpress.kkaravitis.banking.transfer.domain.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.transfer.domain.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.transfer.domain.TransactionalOutbox.TransactionalOutboxContext;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaStepHandler.SagaParticipantCommand;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaStepHandler.SagaStepResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class SagaOrchestrator<T extends Enum<T>, S extends SagaStepHandler<T>> {
    private final SagaRepository sagaRepository;
    protected final ObjectMapper objectMapper;
    private final List<S> sagaStepHandlers;
    private final TransactionalOutbox transactionalOutbox;

    protected void handleReply(SagaReplyHandlerContext<T> context) throws JsonProcessingException {
        UUID sagaId = UUID.fromString(context.getSagaIdHeader());

        SagaEntity sagaEntity = sagaRepository.findById(sagaId)
              .orElseThrow(() -> new SagaRuntimeException("Saga not found: " + sagaId));

        final SagaData<T> sagaData = objectMapper.readValue(
              sagaEntity.getSagaDataJson(), context.getSagaDataType()
        );
        SagaEvent event = toDomainEvent(context.getMessageType(),
              context.getPayloadJson());

        Optional<SagaStepResult<T>> optionalSagaStepResult = sagaStepHandlers.stream()
              .filter(handler -> sagaData.getStatus()
                    .equals(handler.currentSagaStatus()))
              .findFirst()
              .map(handler -> handler.handle(event, sagaData));

        if (optionalSagaStepResult.isEmpty()) {
            return;
        }

        SagaStepResult<T> sagaStepResult = optionalSagaStepResult.get();
        SagaData<T> updatedSagaData = sagaStepResult.getSagaData();
        String updatedSagaState = updatedSagaData.getStatus().name();

        sagaEntity.setSagaState(updatedSagaState);
        sagaEntity.setSagaDataJson(objectMapper.writeValueAsString(updatedSagaData));
        sagaRepository.save(sagaEntity);

        SagaParticipantCommand sagaParticipantCommand = sagaStepResult.getSagaParticipantCommand();
        if (sagaParticipantCommand == null) {
            return;
        }

        transactionalOutbox.enqueue(TransactionalOutboxContext
              .builder()
                    .aggregateType(context.getSagaType())
                    .messageType(sagaParticipantCommand.getMessageType())
                    .destinationTopic(sagaParticipantCommand.getDestinationTopic())
                    .aggregateId(sagaId)
                    .headers(Map.of("reply-topic", context.getSagaReplyTopic()))
                    .payload(sagaParticipantCommand)
              .build());
    }

    protected abstract SagaEvent toDomainEvent(String messageType, String payloadJson) throws JsonProcessingException;

}