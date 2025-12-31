package com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.transfer.domain.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaOrchestrator;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaReplyHandlerContext;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaRepository;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaRuntimeException;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.events.FundsReservationCancellationRejectedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.cancellation.events.FundsReservationCancelledEvent;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferCancellationSagaOrchestrator extends SagaOrchestrator<TransferCancellationSagaStatus, TransferCancellationSagaStepHandler> {

    public TransferCancellationSagaOrchestrator(
          SagaRepository sagaRepository,
          ObjectMapper objectMapper,
          List<TransferCancellationSagaStepHandler> handlers,
          TransactionalOutbox transactionalOutboxPort) {
        super(sagaRepository, objectMapper, handlers, transactionalOutboxPort);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void onReply(
          String messageType,
          String sagaIdHeader,
          String payloadJson
    ) {

        SagaReplyHandlerContext<TransferCancellationSagaStatus> context =
              SagaReplyHandlerContext.<TransferCancellationSagaStatus>builder()
                    .messageType(messageType)
                    .sagaIdHeader(sagaIdHeader)
                    .payloadJson(payloadJson)
                    .sagaDataType(TransferCancellationSagaData.class)
                    .sagaType("TransferCancellationSaga")
                    .sagaReplyTopic("transfer-cancellation-saga-replies")
                    .build();

        try {
            super.handleReply(context);
        } catch (JsonProcessingException jsonProcessingException) {
            throw new SagaRuntimeException("Fatal error", jsonProcessingException);
        }
    }

    protected SagaEvent toDomainEvent(String messageType, String payloadJson) throws JsonProcessingException {
        return switch (messageType) {
            case "FundsReservationCancelled" ->
                  objectMapper.readValue(payloadJson, FundsReservationCancelledEvent.class);
            case "FundsReservationCancellationRejected" ->
                  objectMapper.readValue(payloadJson, FundsReservationCancellationRejectedEvent.class);
            default ->
                  throw new IllegalArgumentException("Unknown reply type: " + messageType);
        };
    }
}