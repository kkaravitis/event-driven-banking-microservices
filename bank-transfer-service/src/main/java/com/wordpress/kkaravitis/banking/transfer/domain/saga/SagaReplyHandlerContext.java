package com.wordpress.kkaravitis.banking.transfer.domain.saga;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SagaReplyHandlerContext<T extends Enum<T>> {
    final String messageType;
    final String sagaIdHeader;
    final String payloadJson;
    final Class<? extends SagaData<T>> sagaDataType;
    final String sagaType;
    final String sagaReplyTopic;
}
