package com.wordpress.kkaravitis.banking.account.adapters;

import com.wordpress.kkaravitis.banking.account.application.AccountCommand;
import com.wordpress.kkaravitis.banking.account.application.AccountCommandHandlerService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MessagingAdapter {
    private static final String MESSAGE_ID_HEADER = "x-message-id";
    private static final String AGGREGATE_ID_HEADER = "x-aggregate-id";
    private static final String AGGREGATE_TYPE_HEADER = "x-aggregate-type";
    private static final String MESSAGE_TYPE_HEADER = "x-message-type";
    private static final String REPLY_TOPIC_HEADER = "x-reply-topic";

    private final AccountCommandHandlerService accountCommandHandlerService;

    @KafkaListener(topics = "${app.kafka.account-commands-topic}")
    public void handleAccountCommand(
          @Header(MESSAGE_ID_HEADER) String messageId,
          @Header(AGGREGATE_ID_HEADER) String aggregateId,
          @Header(MESSAGE_TYPE_HEADER) String messageType,
          @Header(REPLY_TOPIC_HEADER) String replyTopic,
          @Header(AGGREGATE_TYPE_HEADER) String aggregateType,
          @Payload String payload) {
        accountCommandHandlerService.handle(AccountCommand.builder()
                    .message(payload)
                    .messageType(messageType)
                    .messageId(messageId)
                    .aggregateId(UUID.fromString(aggregateId))
                    .aggregateType(aggregateType)
                    .replyTopic(replyTopic)
              .build());
    }
}
