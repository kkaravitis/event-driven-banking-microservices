package com.wordpress.kkaravitis.banking.antifraud.adapter;

import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.CORRELATION_ID_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.MESSAGE_ID_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.MESSAGE_TYPE_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.REPLY_TOPIC_HEADER;

import com.wordpress.kkaravitis.banking.antifraud.api.commands.CheckFraudCommand;
import com.wordpress.kkaravitis.banking.antifraud.application.AntiFraudService;
import com.wordpress.kkaravitis.banking.antifraud.application.CheckFraudCommandContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessagingAdapter {
    private final AntiFraudService antiFraudService;

    @KafkaListener(
          topics = "${app.kafka.topics.anti-fraud-service-commands-topic}",
          containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(
          CheckFraudCommand command,
          @Header(MESSAGE_ID_HEADER) String messageId,
          @Header(REPLY_TOPIC_HEADER) String replyTopic,
          @Header(CORRELATION_ID_HEADER) String correlationId,
          @Header(MESSAGE_TYPE_HEADER) String inboundMessageType
    ) {

        if (!CheckFraudCommand.MESSAGE_TYPE.equals(inboundMessageType)) {
            throw new IllegalArgumentException(
                  "Unsupported messageType=" + inboundMessageType + " for payload CheckFraudCommand"
            );
        }

        antiFraudService.handleCheckFraudCommand(CheckFraudCommandContext.builder()
                    .messageId(messageId)
                    .checkFraudCommand(command)
                    .destinationTopic(replyTopic)
                    .correlationId(UUID.fromString(correlationId))
              .build());
    }
}
