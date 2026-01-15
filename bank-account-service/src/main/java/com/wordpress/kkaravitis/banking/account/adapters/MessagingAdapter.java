package com.wordpress.kkaravitis.banking.account.adapters;

import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.CORRELATION_ID_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.MESSAGE_ID_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.MESSAGE_TYPE_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.REPLY_TOPIC_HEADER;

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

    private final AccountCommandHandlerService accountCommandHandlerService;

    @KafkaListener(topics = "${app.kafka.topics.accounts-service-commands-topic}")
    public void handleAccountCommand(
          @Header(MESSAGE_ID_HEADER) String messageId,
          @Header(CORRELATION_ID_HEADER) String correlationId,
          @Header(MESSAGE_TYPE_HEADER) String messageType,
          @Header(REPLY_TOPIC_HEADER) String replyTopic,
          @Payload String payload) {
        accountCommandHandlerService.handle(AccountCommand.builder()
                    .message(payload)
                    .messageType(messageType)
                    .messageId(messageId)
                    .correlationId(UUID.fromString(correlationId))
                    .replyTopic(replyTopic)
              .build());
    }
}
