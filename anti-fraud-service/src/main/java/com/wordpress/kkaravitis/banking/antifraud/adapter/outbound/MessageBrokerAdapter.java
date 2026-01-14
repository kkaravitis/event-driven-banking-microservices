package com.wordpress.kkaravitis.banking.antifraud.adapter.outbound;

import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.CORRELATION_ID_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.MESSAGE_ID_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.MESSAGE_TYPE_HEADER;

import com.wordpress.kkaravitis.banking.antifraud.application.MessageBrokerPort;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MessageBrokerAdapter implements MessageBrokerPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void send(OutboundMessage outboundMessage) {
        Message<Object> message = MessageBuilder.withPayload(outboundMessage.getPayload())
              .setHeader(KafkaHeaders.TOPIC, outboundMessage.getDestinationTopic())
              .setHeader(KafkaHeaders.KEY, outboundMessage.getCorrelationId())
              .setHeader(MESSAGE_ID_HEADER, outboundMessage.getMessageId())
              .setHeader(CORRELATION_ID_HEADER, outboundMessage.getCorrelationId())
              .setHeader(MESSAGE_TYPE_HEADER, outboundMessage.getMessageType())
              .build();

        kafkaTemplate.send(message);
    }
}
