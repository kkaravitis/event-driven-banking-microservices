package com.wordpress.kkaravitis.banking.account.application;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountCommand {
    private final String messageType;
    private final String messageId;
    private final String message;
    private final UUID aggregateId;
    private final String aggregateType;
    private final String replyTopic;
}
