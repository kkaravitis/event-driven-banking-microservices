package com.wordpress.kkaravitis.banking.antifraud.application;

import com.wordpress.kkaravitis.banking.antifraud.AntiFraudService;
import com.wordpress.kkaravitis.banking.antifraud.api.commands.CheckFraudCommand;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudApprovedEvent;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudEventType;
import com.wordpress.kkaravitis.banking.antifraud.api.events.FraudRejectedEvent;
import com.wordpress.kkaravitis.banking.antifraud.application.MessageBrokerPort.OutboundMessage;
import com.wordpress.kkaravitis.banking.antifraud.domain.BlacklistCheckService;
import com.wordpress.kkaravitis.banking.antifraud.domain.FraudDecision;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AntiFraudAppService implements AntiFraudService {
    private final BlacklistCheckService blacklistCheckService;
    private final MessageBrokerPort messageBrokerPort;


    @Override
    public void handleCheckFraudCommand(CheckFraudCommandContext context) {
        CheckFraudCommand command = context.getCheckFraudCommand();
        FraudDecision decision = blacklistCheckService
              .check(command.getFromAccountId(), command.getToAccountId());

        String messageType;
        Object payload;
        if (decision.approved()) {
            messageType = FraudEventType.FRAUD_APPROVED.getMessageType();
            payload = new FraudApprovedEvent(command.getTransferId());
        } else {
            messageType = FraudEventType.FRAUD_REJECTED.getMessageType();
            payload = new FraudRejectedEvent(command.getTransferId(), decision.reason());
        }
        String newMessageId = UUID.randomUUID().toString();

        messageBrokerPort.send(OutboundMessage.builder()
                    .messageId(newMessageId)
                    .correlationId(context.getCorrelationId())
                    .destinationTopic(context.getReplyTopic())
                    .payload(payload)
                    .messageType(messageType)
              .build());
    }
}
