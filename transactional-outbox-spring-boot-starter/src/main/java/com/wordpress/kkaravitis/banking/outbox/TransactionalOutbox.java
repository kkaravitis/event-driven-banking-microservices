package com.wordpress.kkaravitis.banking.outbox;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * API for enqueuing outbox messages within the current business transaction.
 */
public interface TransactionalOutbox {

    void enqueue(TransactionalOutboxContext context);

    @Builder
    @Getter
    class TransactionalOutboxContext {
        private final UUID correlationId;
        private final String messageType;
        private final Object payload;
        private final String destinationTopic;
        private final String replyTopic;
    }
}
