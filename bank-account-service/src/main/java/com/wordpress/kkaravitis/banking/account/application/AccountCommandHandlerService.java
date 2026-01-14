package com.wordpress.kkaravitis.banking.account.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordpress.kkaravitis.banking.account.api.commands.AccountCommandType;
import com.wordpress.kkaravitis.banking.account.api.commands.CancelFundsReservationCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.FinalizeTransferCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.ReleaseFundsCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.ReserveFundsCommand;
import com.wordpress.kkaravitis.banking.account.domain.AccountService;
import com.wordpress.kkaravitis.banking.account.domain.values.DomainEvent;
import com.wordpress.kkaravitis.banking.idempotency.inbox.InboxService;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox;
import com.wordpress.kkaravitis.banking.outbox.TransactionalOutbox.TransactionalOutboxContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class AccountCommandHandlerService {

    private final AccountService accountService;
    private final TransactionalOutbox transactionalOutbox;
    private final InboxService inboxService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle (AccountCommand command) {
            if (!inboxService.validateAndStore(command.getMessageId())) {
                return;
            }
            AccountCommandType accountCommandType = AccountCommandType.valueOf(command.getMessageType());

            DomainEvent domainEvent = switch (accountCommandType) {
                case CANCEL_FUNDS_RESERVATION -> accountService
                      .cancelFundsReservation(toCommand(command.getMessage(),
                            CancelFundsReservationCommand.class));
                case RESERVE_FUNDS -> accountService
                      .reserveFunds(toCommand(command.getMessage(),
                            ReserveFundsCommand.class));
                case RELEASE_FUNDS -> accountService
                      .releaseFunds(toCommand(command.getMessage(),
                            ReleaseFundsCommand.class));
                case FINALIZE_TRANSFER -> accountService
                      .finalizeTransfer(toCommand(command.getMessage(),
                            FinalizeTransferCommand.class));
            };

            transactionalOutbox.enqueue(TransactionalOutboxContext
                  .builder()
                  .messageType(domainEvent.type())
                  .payload(domainEvent.payload())
                  .correlationId(command.getAggregateId())
                  .destinationTopic(command.getReplyTopic())
                  .build());
    }

    private <T> T toCommand(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
          throw new IllegalStateException(exception);
        }
    }
}
