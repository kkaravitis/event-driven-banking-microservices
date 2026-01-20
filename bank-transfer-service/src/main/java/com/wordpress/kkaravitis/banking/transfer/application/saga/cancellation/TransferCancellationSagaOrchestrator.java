package com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.account.api.commands.CancelFundsReservationCommand;
import com.wordpress.kkaravitis.banking.account.api.events.AccountEventType;
import com.wordpress.kkaravitis.banking.common.BankingEventType;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox.TransactionalOutboxContext;
import com.wordpress.kkaravitis.banking.transfer.TransferService.InitiateCancellationCommand;
import com.wordpress.kkaravitis.banking.transfer.TransferService.SagaParticipantReply;
import com.wordpress.kkaravitis.banking.transfer.application.ports.SagaStore;
import com.wordpress.kkaravitis.banking.transfer.application.ports.TransferStore;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaEntity;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaOrchestrator;
import com.wordpress.kkaravitis.banking.transfer.application.saga.SagaReplyHandlerContext;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainError;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainErrorCode;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import com.wordpress.kkaravitis.banking.transfer.domain.Transfer;
import com.wordpress.kkaravitis.banking.transfer.domain.TransferState;
import com.wordpress.kkaravitis.banking.transfer.infrastructure.kafka.Topics;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferCancellationSagaOrchestrator extends SagaOrchestrator<TransferCancellationSagaStatus, TransferCancellationSagaStepHandler> {

    public static final String TRANSFER_CANCELLATION_SAGA = "TransferCancellationSaga";
    private final Topics topics;

    public TransferCancellationSagaOrchestrator(
          Topics topics,
          SagaStore sagaStore,
          TransferStore transferStore,
          ObjectMapper objectMapper,
          List<TransferCancellationSagaStepHandler> handlers,
          TransactionalOutbox transactionalOutboxPort) {
        super(sagaStore, transferStore, objectMapper, handlers, transactionalOutboxPort);
        this.topics = topics;
    }

    @Transactional
    public DomainResult start(InitiateCancellationCommand command) {
        Optional<Transfer> storeResult = transferStore.load(command.getTransferId());
        if (storeResult.isEmpty()) {
            return DomainResult.builder()
                  .error(new DomainError(DomainErrorCode.NOT_EXISTING,
                        String.format("The Transfer entity with id %s was not found during Transfer Cancellation",
                              command.getTransferId())))
                  .build();
        }

        Transfer transfer = storeResult.get();

        DomainResult domainResult = transfer.startCancellation();
        transferStore.save(transfer);
        TransferCancellationSagaStatus sagaStatus;
        if (domainResult.isValid()) {
            sagaStatus = TransferCancellationSagaStatus.CANCEL_PENDING;
        } else {
            DomainError domainError = domainResult.getError();
            if (domainError.code() == DomainErrorCode.CANCEL_TOO_LATE) {
                sagaStatus = TransferCancellationSagaStatus.REJECTED;
            } else {
                sagaStatus = TransferCancellationSagaStatus.FAILED;
            }
        }
        UUID transferId = transfer.getId();
        UUID sagaId = UUID.randomUUID();
        TransferCancellationSagaData sagaData = TransferCancellationSagaData.builder()
              .sagaId(sagaId)
              .transferId(transferId)
              .customerId(command.getCustomerId())
              .status(sagaStatus)
              .build();
        String sagaDataJson = writeJson(sagaData);
        SagaEntity sagaEntity = new SagaEntity(
              sagaId,
              TRANSFER_CANCELLATION_SAGA,
              sagaData.getStatus().name(),
              sagaDataJson
        );
        sagaStore.save(sagaEntity);

        if (!domainResult.isValid()) {
            return domainResult;
        }

        transactionalOutbox.enqueue(TransactionalOutboxContext.builder()
              .correlationId(sagaId)
              .messageType(CancelFundsReservationCommand.MESSAGE_TYPE)
              .payload(CancelFundsReservationCommand.builder()
                    .transferId(transferId)
                    .customerId(command.getCustomerId())
                    .build())
              .destinationTopic(topics.accountsServiceCommandsTopic())
              .replyTopic(topics.transferCancellationSagaRepliesTopic())
              .build());

        return DomainResult.builder()
              .transferId(transferId)
              .build();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void onReply(SagaParticipantReply reply) {
        super.handleReply(SagaReplyHandlerContext.<TransferCancellationSagaStatus>builder()
              .sagaIdHeader(reply.sagaId())
              .messageType(reply.messageType())
              .payloadJson(reply.payloadJson())
              .sagaDataType(TransferCancellationSagaData.class)
              .sagaReplyTopic(topics.transferCancellationSagaRepliesTopic())
              .build());
    }

    @Override
    protected List<BankingEventType> expectedBankingEventTypes() {
        return List.of(AccountEventType.FUNDS_RESERVATION_CANCELLED,
              AccountEventType.FUNDS_RESERVATION_CANCELLATION_REJECTED);
    }

    @Override
    protected TransferCancellationSagaStatus getSagaFailedStatus() {
        return TransferCancellationSagaStatus.FAILED;
    }
}