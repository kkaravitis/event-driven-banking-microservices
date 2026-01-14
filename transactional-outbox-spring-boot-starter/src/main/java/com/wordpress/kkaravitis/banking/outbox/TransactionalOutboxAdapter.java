package com.wordpress.kkaravitis.banking.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation that persists a row in outbox_message.
 */
@RequiredArgsConstructor
public class TransactionalOutboxAdapter implements TransactionalOutbox {
    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void enqueue(TransactionalOutboxContext context) {
        OutboxMessage outboxMessage = OutboxMessage.builder()
              .messageId(UUID.randomUUID())
              .correlationId(context.getCorrelationId())
              .messageType(context.getMessageType())
              .payload(toJson(context.getPayload()))
              .destinationTopic(context.getDestinationTopic())
              .replyTopic(context.getReplyTopic())
              .createdAt(Instant.now())
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
