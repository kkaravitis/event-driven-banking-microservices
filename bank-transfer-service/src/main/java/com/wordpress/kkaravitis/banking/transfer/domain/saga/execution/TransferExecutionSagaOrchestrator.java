package com.wordpress.kkaravitis.banking.transfer.domain.saga.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.transfer.domain.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaOrchestrator;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaReplyHandlerContext;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaRepository;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaRuntimeException;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FraudRejectedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FundsReleasedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FundsReservationFailedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.FundsReservedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.TransferApprovalFailedEvent;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.events.TransferFinalizedEvent;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferExecutionSagaOrchestrator extends SagaOrchestrator<TransferExecutionSagaStatus, TransferExecutionSagaStepHandler> {

    public TransferExecutionSagaOrchestrator(
          SagaRepository sagaRepository,
          ObjectMapper objectMapper,
          List<TransferExecutionSagaStepHandler> handlers,
          TransactionalOutbox transactionalOutboxPort) {
        super(sagaRepository, objectMapper, handlers, transactionalOutboxPort);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void onReply(
          String messageType,
          String sagaIdHeader,
          String payloadJson
    ) throws JsonProcessingException {

        SagaReplyHandlerContext<TransferExecutionSagaStatus> context =
              SagaReplyHandlerContext.<TransferExecutionSagaStatus>builder()
                    .messageType(messageType)
                    .sagaIdHeader(sagaIdHeader)
                    .payloadJson(payloadJson)
                    .sagaDataType(TransferExecutionSagaData.class)
                    .sagaType("TransferExecutionSaga")
                    .sagaReplyTopic("transfer-execution-saga-replies")
                    .build();

        super.handleReply(context);
    }

    protected SagaEvent toDomainEvent(String messageType, String payloadJson) throws JsonProcessingException {
        return switch (messageType) {
            case "FraudApproved" ->
                  objectMapper.readValue(payloadJson, FraudApprovedEvent.class);
            case "FraudRejected" ->
                  objectMapper.readValue(payloadJson, FraudRejectedEvent.class);
            case "FundsReserved" ->
                  objectMapper.readValue(payloadJson, FundsReservedEvent.class);
            case "FundsReservationFailed" ->
                  objectMapper.readValue(payloadJson, FundsReservationFailedEvent.class);
            case "TransferFinalized" ->
                  objectMapper.readValue(payloadJson, TransferFinalizedEvent.class);
            case "TransferApprovalFailed" ->
                  objectMapper.readValue(payloadJson, TransferApprovalFailedEvent.class);
            case "FundsReleased" ->
                  objectMapper.readValue(payloadJson, FundsReleasedEvent.class);
            default ->
                  throw new SagaRuntimeException("Unknown reply type: " + messageType);
        };
    }
}