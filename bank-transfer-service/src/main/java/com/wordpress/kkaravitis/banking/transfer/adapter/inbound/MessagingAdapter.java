package com.wordpress.kkaravitis.banking.transfer.adapter.inbound;

import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.CORRELATION_ID_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.MESSAGE_ID_HEADER;
import static com.wordpress.kkaravitis.banking.common.MessagingContractUtils.MESSAGE_TYPE_HEADER;

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
    public void handleExecutionSagaReplies(
          @Header(MESSAGE_ID_HEADER) String messageId,
          @Header(CORRELATION_ID_HEADER) String correlationId,
          @Header(MESSAGE_TYPE_HEADER) String messageType,
          @Payload String payload) {
         transferService.handleTransferExecutionParticipantReply(new SagaParticipantReply(
                  messageId,
                  correlationId,
                  messageType,
                  payload
            ));
    }

    @KafkaListener(topics = "${app.kafka.transfer-cancellation-saga-replies-topic}")
    public void handleCancellationSagaReplies(
          @Header(MESSAGE_ID_HEADER) String messageId,
          @Header(CORRELATION_ID_HEADER) String correlationId,
          @Header(MESSAGE_TYPE_HEADER) String messageType,
          @Payload String payload) {
            transferService.handleTransferCancellationParticipantReply(new SagaParticipantReply(
                  messageId,
                  correlationId,
                  messageType,
                  payload
            ));
        }
}
