package com.wordpress.kkaravitis.banking.antifraud.application;

import com.wordpress.kkaravitis.banking.antifraud.api.commands.CheckFraudCommand;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudEventType;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudRejectedEvent;
import com.wordpress.kkaravitis.banking.antifraud.domain.BlacklistCheckService;
import com.wordpress.kkaravitis.banking.antifraud.domain.FraudDecision;
import com.wordpress.kkaravitis.banking.idempotency.inbox.InboxService;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox.TransactionalOutboxContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AntiFraudService {
    private final BlacklistCheckService blacklistCheckService;
    private final TransactionalOutbox outbox;
    private final InboxService inboxService;

    @Transactional
    public void handleCheckFraudCommand(CheckFraudCommandContext context) {

        if (!inboxService.validateAndStore(context.getMessageId())) {
            return;
        }

        CheckFraudCommand command = context.getCheckFraudCommand();
        FraudDecision decision = blacklistCheckService
              .check(command.fromAccountId(), command.toAccountId());

        String messageType;
        Object payload;
        if (decision.approved()) {
            messageType = FraudEventType.FRAUD_APPROVED.getMessageType();
            payload = new FraudApprovedEvent(command.transferId());
        } else {
            messageType = FraudEventType.FRAUD_REJECTED.getMessageType();
            payload = new FraudRejectedEvent(command.transferId(), decision.reason());
        }

        outbox.enqueue(TransactionalOutboxContext.builder()
              .destinationTopic(context.getDestinationTopic())
              .payload(payload)
              .messageType(messageType)
              .correlationId(context.getCorrelationId())
              .build());
    }
}
