package com.wordpress.kkaravitis.banking.transfer.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.domain.TransactionalOutbox.TransactionalOutboxContext;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaEntity;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaRepository;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.SagaRuntimeException;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaData;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.TransferExecutionSagaStatus;
import com.wordpress.kkaravitis.banking.transfer.domain.saga.execution.commands.CheckFraudCommand;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final SagaRepository sagaRepository;
    private final TransactionalOutbox transactionalOutboxPort;
    private final ObjectMapper objectMapper;

    @Transactional
    public DomainResult startTransfer(InitiateTransferCommand command) {
        UUID transferId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();

        // create new transfer
        Transfer transfer = Transfer.createNew(
              transferId,
              command.getFromAccountId(),
              command.getToAccountId(),
              command.getAmount(),
              command.getCurrency()
        );
        transferRepository.save(transfer);

        TransferExecutionSagaData sagaData = TransferExecutionSagaData.builder()
              .sagaId(sagaId)
              .transferId(transferId)
              .fromAccountId(command.getFromAccountId())
              .toAccountId(command.getToAccountId())
              .amount(command.getAmount())
              .currency(command.getCurrency())
              .status(TransferExecutionSagaStatus.FRAUD_CHECK_PENDING)
              .fraudDecision("UNKNOWN")
              .build();
        String sagaDataJson = writeJson(sagaData);
        SagaEntity sagaEntity = new SagaEntity(
              sagaId,
              "InternalTransferSaga",
              sagaData.getStatus().name(),
              sagaDataJson
        );
        sagaRepository.save(sagaEntity);

        CheckFraudCommand checkFraudCommand = CheckFraudCommand.builder()
              .transferId(transferId)
              .customerId(command.getCustomerId())
              .fromAccountId(command.getFromAccountId())
              .toAccountId(command.getToAccountId())
              .amount(command.getAmount())
              .currency(command.getCurrency())
              .build();

        transactionalOutboxPort.enqueue(TransactionalOutboxContext.builder()
                    .aggregateId(sagaId)
                    .aggregateType("InitiateTransferSaga")
                    .destinationTopic("check-fraud-commands")
                    .messageType("CheckFraudCommand")
                    .payload(checkFraudCommand)
                    .headers(Map.of("reply-topic", "transfer-initiation-saga-replies"))
              .build());

        return DomainResult.builder()
              .transition(new Transition(null,
                    transfer.getState().name()))
              .build();
    }

    @Transactional
    public DomainResult completeTransfer(CompleteTransferCommand cmd) {
        UUID transferId = cmd.getTransferId();
        Optional<Transfer> transferOptional = transferRepository.findById(transferId);
        if (transferOptional.isEmpty()) {
           return DomainResult.builder()
                 .errors(List.of(new DomainError(DomainErrorCode.NOT_EXISTING,
                       String.format("The Transfer entity with id %s was not found during Transfer Completion",
                             cmd.getTransferId()))))
                 .build();
        }
        return transferOptional.get().complete();
    }

    @Transactional
    public DomainResult rejectTransfer(RejectTransferCommand cmd) {
        UUID transferId = cmd.getTransferId();
        Optional<Transfer> transferOptional = transferRepository.findById(transferId);
        if (transferOptional.isEmpty()) {
            return DomainResult.builder()
                  .errors(List.of(new DomainError(DomainErrorCode.NOT_EXISTING,
                        String.format("The Transfer entity with id %s was not found during Transfer Rejection",
                              cmd.getTransferId()))))
                  .build();
        }
        return transferOptional.get().reject();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new SagaRuntimeException("Failed to serialize JSON", e);
        }
    }
}
