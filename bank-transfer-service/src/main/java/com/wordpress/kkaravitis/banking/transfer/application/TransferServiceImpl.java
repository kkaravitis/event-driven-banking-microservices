package com.wordpress.kkaravitis.banking.transfer.application;

import com.wordpress.kkaravitis.banking.idempotency.inbox.InboxService;
import com.wordpress.kkaravitis.banking.transfer.TransferService;
import com.wordpress.kkaravitis.banking.transfer.application.saga.cancellation.TransferCancellationSagaOrchestrator;
import com.wordpress.kkaravitis.banking.transfer.application.saga.execution.TransferExecutionSagaOrchestrator;
import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TransferServiceImpl implements TransferService {

    private final InboxService inboxService;
    private final TransferExecutionSagaOrchestrator transferExecutionSagaOrchestrator;
    private final TransferCancellationSagaOrchestrator transferCancellationSagaOrchestrator;

    @Transactional
    public DomainResult startTransfer(InitiateTransferCommand command) {
       return transferExecutionSagaOrchestrator.start(command);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public DomainResult startCancellation(InitiateCancellationCommand command) {
        return transferCancellationSagaOrchestrator.start(command);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void handleTransferExecutionParticipantReply(SagaParticipantReply reply) {
        if (!inboxService.validateAndStore(reply.messageId())) {
            return;
        }
        transferExecutionSagaOrchestrator.onReply(reply);

    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void handleTransferCancellationParticipantReply(SagaParticipantReply reply) {
        if (!inboxService.validateAndStore(reply.messageId())) {
            return;
        }

        transferCancellationSagaOrchestrator.onReply(reply);
    }
}
