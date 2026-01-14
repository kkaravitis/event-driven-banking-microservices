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
    private final UUID correlationId;
    private final String replyTopic;
}
