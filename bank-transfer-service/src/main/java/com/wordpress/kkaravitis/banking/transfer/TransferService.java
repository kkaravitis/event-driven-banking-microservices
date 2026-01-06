package com.wordpress.kkaravitis.banking.transfer;

import com.wordpress.kkaravitis.banking.transfer.domain.AggregateResult;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

public interface TransferService {

    AggregateResult startTransfer(InitiateTransferCommand command);

    AggregateResult startCancellation(InitiateCancellationCommand command);

    void handleTransferCancellationParticipantReply(SagaParticipantReply reply);

    void handleTransferExecutionParticipantReply(SagaParticipantReply reply);

    @Getter
    @Builder
    class InitiateTransferCommand {
        private String customerId;
        private String fromAccountId;
        private String toAccountId;
        private BigDecimal amount;
        private String currency;
    }

    @Builder
    @Getter
    class InitiateCancellationCommand {
        private String customerId;
        private UUID transferId;
    }

    record SagaParticipantReply(
          String messageId,
          String sagaId,
          String messageType,
          String payloadJson) {
    }
}
