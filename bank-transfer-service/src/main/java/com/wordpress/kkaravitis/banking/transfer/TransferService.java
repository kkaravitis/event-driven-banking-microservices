package com.wordpress.kkaravitis.banking.transfer;

import com.wordpress.kkaravitis.banking.transfer.domain.DomainResult;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

public interface TransferService {
    DomainResult startTransfer(InitiateTransferCommand command);

    DomainResult completeTransfer(CompleteTransferCommand command);

    DomainResult rejectTransfer(RejectTransferCommand command);

    @Getter
    @Builder
    class InitiateTransferCommand {
        private String customerId;
        private String fromAccountId;
        private String toAccountId;
        private BigDecimal amount;
        private String currency;
    }

    @Getter
    @Builder
    class RejectTransferCommand {
        private UUID transferId;
        private String reason;
    }

    @Getter
    @Builder
    class CompleteTransferCommand {
        private UUID transferId;
    }
}
