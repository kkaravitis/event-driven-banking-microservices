package com.wordpress.kkaravitis.banking.transfer.domain;

import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

public interface TransactionalOutbox {

    void enqueue(TransactionalOutboxContext context);

    @Getter
    @Builder
    class TransactionalOutboxContext {
        private String aggregateType;
        private UUID aggregateId;
        private String destinationTopic;
        private String messageType;
        private Map<String, String> headers;
        private Object payload;
    }
}
