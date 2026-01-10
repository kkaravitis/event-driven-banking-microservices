package com.wordpress.kkaravitis.banking.transfer.adapter.inbound;

import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.TransferService.SagaParticipantReply;
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
    private static final String MESSAGE_TYPE_HEADER = "x-message-type";

    private final TransferService transferService;

    @KafkaListener(topics = "${app.kafka.transfer-execution-saga-replies-topic}")
    public void handleExecutionSagaReplies(
          @Header(MESSAGE_ID_HEADER) String messageId,
          @Header(AGGREGATE_ID_HEADER) String aggregateId,
          @Header(MESSAGE_TYPE_HEADER) String messageType,
          @Payload String payload) {
         transferService.handleTransferExecutionParticipantReply(new SagaParticipantReply(
                  messageId,
                  aggregateId,
                  messageType,
                  payload
            ));
    }

    @KafkaListener(topics = "${app.kafka.transfer-cancellation-saga-replies-topic}")
    public void handleCancellationSagaReplies(
          @Header(MESSAGE_ID_HEADER) String messageId,
          @Header(AGGREGATE_ID_HEADER) String aggregateId,
          @Header(MESSAGE_TYPE_HEADER) String messageType,
          @Payload String payload) {
            transferService.handleTransferCancellationParticipantReply(new SagaParticipantReply(
                  messageId,
                  aggregateId,
                  messageType,
                  payload
            ));
        }
}
