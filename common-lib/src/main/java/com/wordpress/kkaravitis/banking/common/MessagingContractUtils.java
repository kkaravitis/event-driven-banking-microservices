package com.wordpress.kkaravitis.banking.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessagingContractUtils {
    public static final String MESSAGE_ID_HEADER = "x-message-id";
    public static final String CORRELATION_ID_HEADER = "x-correlation-id";
    public static final String MESSAGE_TYPE_HEADER = "x-message-type";
    public static final String REPLY_TOPIC_HEADER = "x-reply-topic";
}
