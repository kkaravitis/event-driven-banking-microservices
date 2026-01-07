package com.wordpress.kkaravitis.banking.transfer.adapter.outbound.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransactionalOutbox;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TransactionalOutboxAdapter implements TransactionalOutbox {
    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void enqueue(TransactionalOutboxContext context) {
        OutboxMessage outboxMessage = OutboxMessage.builder()
              .messageType(context.getMessageType())
              .createdAt(Instant.now())
              .correlationId(context.getAggregateId())
              .destinationTopic(context.getDestinationTopic())
              .payload(toJson(context.getPayload()))
              .build();

        outboxMessageRepository.save(outboxMessage);
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
