package com.wordpress.kkaravitis.banking.account.domain.factory;

import com.wordpress.kkaravitis.banking.account.api.commands.CancelFundsReservationCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.FinalizeTransferCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.ReleaseFundsCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.ReserveFundsCommand;
import com.wordpress.kkaravitis.banking.account.api.events.incident.AccountServiceIncidentAction;
import com.wordpress.kkaravitis.banking.account.api.events.incident.AccountServiceIncidentEvent;
import com.wordpress.kkaravitis.banking.account.api.events.incident.AccountServiceIncidentReason;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceIncidentEventFactory {
    public AccountServiceIncidentEvent build(AccountServiceIncidentAction action,
          AccountServiceIncidentReason reason,
          String message,
          ReserveFundsCommand command) {
        return AccountServiceIncidentEvent.builder()
              .transferId(command.getTransferId())
              .fromAccountId(command.getFromAccountId())
              .toAccountId(command.getToAccountId())
              .customerId(command.getCustomerId())
              .occurredAt(Instant.now())
              .action(action)
              .reason(reason)
              .reservationId(null)
              .message(message)
              .build();
    }

    public AccountServiceIncidentEvent build(AccountServiceIncidentAction action,
          AccountServiceIncidentReason reason,
          String message,
          ReleaseFundsCommand command) {
        return AccountServiceIncidentEvent.builder()
              .transferId(command.getTransferId())
              .fromAccountId(command.getFromAccountId())
              .customerId(command.getCustomerId())
              .occurredAt(Instant.now())
              .action(action)
              .reason(reason)
              .reservationId(command.getReservationId())
              .message(message)
              .build();
    }

    public AccountServiceIncidentEvent build(AccountServiceIncidentAction action,
          AccountServiceIncidentReason reason,
          String message,
          FinalizeTransferCommand command) {
        return AccountServiceIncidentEvent.builder()
              .transferId(command.getTransferId())
              .customerId(command.getCustomerId())
              .occurredAt(Instant.now())
              .action(action)
              .reason(reason)
              .message(message)
              .reservationId(command.getReservationId())
              .build();
    }

    public AccountServiceIncidentEvent build(AccountServiceIncidentAction action,
          AccountServiceIncidentReason reason,
          String message,
          CancelFundsReservationCommand command) {
        return AccountServiceIncidentEvent.builder()
              .transferId(command.getTransferId())
              .customerId(command.getCustomerId())
              .occurredAt(Instant.now())
              .action(action)
              .reason(reason)
              .message(message)
              .build();
    }
}
