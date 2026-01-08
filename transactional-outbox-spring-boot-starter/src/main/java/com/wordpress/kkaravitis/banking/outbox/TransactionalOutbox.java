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
        private final String aggregateType;
        private final UUID aggregateId;
        private final String destinationTopic;
        private final String messageType;
        private final String replyTopic;
        private final Object payload;
    }
}
