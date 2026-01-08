package com.wordpress.kkaravitis.banking.transfer.adapter.inbound;

import com.wordpress.kkaravitis.banking.idempotency.inbox.InboxService;
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
    private final TransferService transferService;

    @KafkaListener(topics = "${app.kafka.transfer-execution-saga-replies-topic}")
    public void hadleExecutionSagaReplies(
          @Header("x-message-id") String messageId,
          @Header("x-aggregate-id") String aggregateId,
          @Header("x-message-type") String messageType,
          @Payload String payload) {
         transferService.handleTransferExecutionParticipantReply(new SagaParticipantReply(
                  messageId,
                  aggregateId,
                  messageType,
                  payload
            ));
    }

    @KafkaListener(topics = "${app.kafka.transfer-cancellation-saga-replies-topic}")
    public void hadleCancellationSagaReplies(
          @Header("x-message-id") String messageId,
          @Header("x-aggregate-id") String aggregateId,
          @Header("x-message-type") String messageType,
          @Payload String payload) {
            transferService.handleTransferCancellationParticipantReply(new SagaParticipantReply(
                  messageId,
                  aggregateId,
                  messageType,
                  payload
            ));
        }
}
