package com.wordpress.kkaravitis.banking.account.domain;

import com.wordpress.kkaravitis.banking.account.api.commands.CancelFundsReservationCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.FinalizeTransferCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.ReleaseFundsCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.ReserveFundsCommand;
import com.wordpress.kkaravitis.banking.account.api.events.AccountEventType;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReleaseFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReleasedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationCancelledEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationFailedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.TransferApprovalFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.account.api.events.TransferApprovalFailedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.TransferFinalizedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.incident.AccountServiceIncidentAction;
import com.wordpress.kkaravitis.banking.account.api.events.incident.AccountServiceIncidentReason;
import com.wordpress.kkaravitis.banking.account.domain.entity.AbortedTransfer;
import com.wordpress.kkaravitis.banking.account.domain.entity.Account;
import com.wordpress.kkaravitis.banking.account.domain.entity.FundsReservation;
import com.wordpress.kkaravitis.banking.account.domain.factory.AccountServiceIncidentEventFactory;
import com.wordpress.kkaravitis.banking.account.domain.repository.AbortedTransferRepository;
import com.wordpress.kkaravitis.banking.account.domain.repository.AccountRepository;
import com.wordpress.kkaravitis.banking.account.domain.repository.FundsReservationRepository;
import com.wordpress.kkaravitis.banking.account.domain.type.DomainErrorCode;
import com.wordpress.kkaravitis.banking.account.domain.type.ReservationStatus;
import com.wordpress.kkaravitis.banking.account.domain.value.DomainEvent;
import com.wordpress.kkaravitis.banking.account.domain.value.DomainResult;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AccountService {

    private static final String TRANSFER_HAD_BEEN_CANCELLED = "Transfer had been cancelled";
    private static final String AUTHORIZATION_ERROR_TEMPLATE = "UNAUTHORIZED: customer %s does not own fromAccount %s. "
          + "From account is owned by customer %s";
    private static final String FROM_ACCOUNT_NOT_FOUND_ERROR_TEMPLATE = "From account with account id %s is not found";
    private static final String TO_ACCOUNT_NOT_FOUND_ERROR_TEMPLATE = "To account with account id %s is not found";
    private static final String RESERVATION_ILLEGAL_STATE_TRANSITION_ERROR_TEMPLATE = "Reservation %s already exists in state %s";
    private static final String RESERVATION_NOT_FOUND_ERROR_TEMPLATE = "Reservation with id %s is not found";
    private static final String RESERVATION_FROM_ACCOUNT_DOES_NOT_EXIST = "Reservation %s holds a from account with id %s that does not exists in accounts.";
    private static final String RESERVATION_TO_ACCOUNT_DOES_NOT_EXIST = "Reservation %s holds a to account with id %s that does not exists in accounts.";
    private static final String UNSUPPORTED_ACCOUNT_EVENT = "Unsupported Account Event of class : %s";
    private static final String TRANSFER_FINALIZED_BEFORE_CANCEL="Transfer %s was finalized while cancellation permitted.";

    private final AccountRepository accountRepository;
    private final FundsReservationRepository reservationRepository;
    private final AbortedTransferRepository abortedTransferRepository;
    private final AccountServiceIncidentEventFactory incidentEventFactory;

    @Transactional
    public DomainEvent reserveFunds(ReserveFundsCommand command) {
        UUID transferId = command.getTransferId();

        if (abortedTransferRepository.existsByTransferIdAndCustomerId(transferId, command.getCustomerId())) {
            return toDomainEvent(new FundsReservationFailedDueToCancelEvent(transferId,
                  TRANSFER_HAD_BEEN_CANCELLED));
        }

        Optional<FundsReservation> existingReservation = reservationRepository.findByTransferId(transferId);
        if (existingReservation.isPresent()) {
            return handleExistingReservation(existingReservation.get(), command);
        }

        Account from = accountRepository.findById(command.getFromAccountId())
              .orElse(null);
        if (from == null) {
            return toDomainEvent(
                  incidentEventFactory.build(AccountServiceIncidentAction.RESERVE_FUNDS,
                        AccountServiceIncidentReason.FROM_ACCOUNT_NOT_FOUND,
                        FROM_ACCOUNT_NOT_FOUND_ERROR_TEMPLATE
                              .formatted(command.getFromAccountId()),
                        command)
            );
        }

        if (!from.isOwnedBy(command.getCustomerId())) {
            return toDomainEvent(incidentEventFactory
                  .build(AccountServiceIncidentAction.RESERVE_FUNDS,
                  AccountServiceIncidentReason.UNAUTHORIZED,
                  AUTHORIZATION_ERROR_TEMPLATE
                        .formatted(command.getCustomerId(), from.getAccountId(), from.getCustomerId()),
                  command)
            );
        }

        Account to = accountRepository.findById(command.getToAccountId()).orElse(null);
        if (to == null) {
            return toDomainEvent(incidentEventFactory
                  .build(AccountServiceIncidentAction.RESERVE_FUNDS,
                        AccountServiceIncidentReason.TO_ACCOUNT_NOT_FOUND,
                        TO_ACCOUNT_NOT_FOUND_ERROR_TEMPLATE
                              .formatted(command.getToAccountId()),
                        command));
        }

        DomainResult domainResult = from.reserve(command.getAmount(),
              command.getCurrency());
        if (!domainResult.isValid()) {
            return toDomainEvent(new FundsReservationFailedEvent(transferId,
                  domainResult.getError().message()));
        }

        String reservationId = UUID.randomUUID().toString();
        FundsReservation reservation = FundsReservation.createNew(
              reservationId,
              transferId,
              command.getFromAccountId(),
              command.getToAccountId(),
              command.getAmount(),
              command.getCurrency()
        );
        reservationRepository.save(reservation);

        return toDomainEvent(new FundsReservedEvent(transferId, reservationId));
    }

    @Transactional
    public DomainEvent releaseFunds(ReleaseFundsCommand command) {
        UUID transferId = command.getTransferId();

        FundsReservation fundsReservation = reservationRepository.findById(command.getReservationId()).orElse(null);
        if (fundsReservation == null) {
            return toDomainEvent(incidentEventFactory.build(
                  AccountServiceIncidentAction.RELEASE_FUNDS,
                  AccountServiceIncidentReason.RESERVATION_NOT_FOUND,
                  RESERVATION_NOT_FOUND_ERROR_TEMPLATE.formatted(command.getReservationId()),
                  command
            ));
        }

        Account from = accountRepository.findById(fundsReservation.getFromAccountId()).orElse(null);
        if (from == null) {
            return toDomainEvent(
                  incidentEventFactory.build(AccountServiceIncidentAction.RELEASE_FUNDS,
                        AccountServiceIncidentReason.FROM_ACCOUNT_NOT_FOUND,
                        RESERVATION_FROM_ACCOUNT_DOES_NOT_EXIST
                              .formatted(fundsReservation.getReservationId(),
                                    fundsReservation.getFromAccountId()),
                        command)
            );
        }

        if (!from.isOwnedBy(command.getCustomerId())) {
            return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.RELEASE_FUNDS,
                  AccountServiceIncidentReason.UNAUTHORIZED,
                  AUTHORIZATION_ERROR_TEMPLATE
                        .formatted(command.getCustomerId(), from.getAccountId(), from.getCustomerId()),
                  command));
        }

        if (abortedTransferRepository.existsByTransferIdAndCustomerId(transferId, command.getCustomerId())
              || fundsReservation.isCancelled()) {
            return toDomainEvent(new FundsReleaseFailedDueToCancelEvent(transferId,
                  fundsReservation.getReservationId()));
        }

        if (fundsReservation.getStatus() == ReservationStatus.RELEASED) {
            return toDomainEvent(new FundsReleasedEvent(transferId, fundsReservation.getReservationId()));
        }

        DomainResult result = fundsReservation.release(from);
        if (!result.isValid()) {
            return toDomainEvent(incidentEventFactory
                  .build(AccountServiceIncidentAction.RELEASE_FUNDS,
                        AccountServiceIncidentReason.INVALID_STATE,
                        result.getError().message(),
                        command));
        }

        return toDomainEvent(new FundsReleasedEvent(transferId,
              fundsReservation.getReservationId()));
    }

    @Transactional
    public DomainEvent finalizeTransfer(FinalizeTransferCommand command) {
        UUID transferId = command.getTransferId();

        FundsReservation reservation = reservationRepository
              .findById(command.getReservationId()).orElse(null);
        if (reservation == null) {
            return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.FINALIZE_TRANSFER,
                  AccountServiceIncidentReason.RESERVATION_NOT_FOUND,
                  RESERVATION_NOT_FOUND_ERROR_TEMPLATE
                        .formatted(command.getReservationId()),
                  command));
        }

        if (abortedTransferRepository.existsByTransferIdAndCustomerId(transferId, command.getCustomerId())
              || reservation.isCancelled()) {
            return toDomainEvent(new TransferApprovalFailedDueToCancelEvent(transferId,
                  TRANSFER_HAD_BEEN_CANCELLED));
        }

        if (reservation.getStatus() == ReservationStatus.FINALIZED) {
            return toDomainEvent(new TransferFinalizedEvent(transferId));
        }

        Account from = accountRepository.findById(reservation.getFromAccountId()).orElse(null);
        if (from == null) {
            return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.FINALIZE_TRANSFER,
                      AccountServiceIncidentReason.FROM_ACCOUNT_NOT_FOUND,
                      RESERVATION_FROM_ACCOUNT_DOES_NOT_EXIST
                            .formatted(reservation.getReservationId(), reservation.getFromAccountId()),
                      command));
        }

        if (!from.isOwnedBy(command.getCustomerId())) {
            return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.FINALIZE_TRANSFER,
                      AccountServiceIncidentReason.UNAUTHORIZED,
                      AUTHORIZATION_ERROR_TEMPLATE.formatted(command.getCustomerId(),
                            from.getAccountId(), from.getCustomerId()),
                      command));
        }

        Account to = accountRepository.findById(reservation.getToAccountId()).orElse(null);
        if (to == null) {
            return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.FINALIZE_TRANSFER,
                  AccountServiceIncidentReason.TO_ACCOUNT_NOT_FOUND,
                  RESERVATION_TO_ACCOUNT_DOES_NOT_EXIST
                        .formatted(reservation.getReservationId(),
                              reservation.getToAccountId()),
                  command));
        }

        DomainResult result = reservation.finalizeTransfer(from, to);
        if (!result.isValid()) {
            DomainErrorCode errorCode = result.getError().code();
            if (errorCode == DomainErrorCode.INSUFFICIENT_AVAILABLE_FUNDS ||
                  errorCode == DomainErrorCode.INSUFFICIENT_RESERVED_FUNDS) {
                return toDomainEvent(new TransferApprovalFailedEvent(transferId,
                      result.getError().message()));
            } else {
                return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.FINALIZE_TRANSFER,
                      AccountServiceIncidentReason.INVALID_STATE,
                      result.getError().message(),
                      command));
            }
        }

        return toDomainEvent(new TransferFinalizedEvent(transferId));
    }

    @Transactional
    public DomainEvent cancelFundsReservation(CancelFundsReservationCommand command) {
        UUID transferId = command.getTransferId();

        markAbortedIfNeeded(command);

        Optional<FundsReservation> opt = reservationRepository.findByTransferId(transferId);
        if (opt.isEmpty()) {
            return toDomainEvent(new FundsReservationCancelledEvent(transferId));
        }

        FundsReservation reservation = opt.get();
        if (reservation.getStatus() == ReservationStatus.FINALIZED) {
            return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.CANCEL_RESERVATION,
                  AccountServiceIncidentReason.TRANSFER_FINALIZED_BEFORE_CANCEL,
                  TRANSFER_FINALIZED_BEFORE_CANCEL
                        .formatted(transferId),
                  command));
        }

        Account from = accountRepository.findById(reservation.getFromAccountId())
              .orElse(null);
        if (from == null) {
            return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.CANCEL_RESERVATION,
              AccountServiceIncidentReason.FROM_ACCOUNT_NOT_FOUND,
              RESERVATION_FROM_ACCOUNT_DOES_NOT_EXIST
                    .formatted(reservation.getReservationId(), reservation.getFromAccountId()),
              command));
        }

        if (!from.isOwnedBy(command.getCustomerId())) {
            return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.CANCEL_RESERVATION,
                  AccountServiceIncidentReason.UNAUTHORIZED,
                  AUTHORIZATION_ERROR_TEMPLATE.formatted(command.getCustomerId(),
                        from.getAccountId(), from.getCustomerId()),
                  command));
        }

        DomainResult domainResult = reservation.cancel(from);

        if (!domainResult.isValid()) {
            return toDomainEvent(incidentEventFactory.build(AccountServiceIncidentAction.CANCEL_RESERVATION,
                  AccountServiceIncidentReason.INVALID_STATE,
                  domainResult.getError().message(),
                  command));
        }

        return toDomainEvent(new FundsReservationCancelledEvent(transferId));
    }

    private DomainEvent handleExistingReservation(FundsReservation reservation, ReserveFundsCommand command) {
        UUID transferId = command.getTransferId();

        if (reservation.getStatus() == ReservationStatus.ACTIVE) {
            return toDomainEvent(new FundsReservedEvent(transferId, reservation.getReservationId()));
        }

        if (reservation.isCancelled()) {
            return toDomainEvent(new FundsReservationFailedDueToCancelEvent(transferId,
                  TRANSFER_HAD_BEEN_CANCELLED));
        }

        return toDomainEvent(
              incidentEventFactory.build(AccountServiceIncidentAction.RESERVE_FUNDS,
                    AccountServiceIncidentReason.INVALID_STATE,
                    RESERVATION_ILLEGAL_STATE_TRANSITION_ERROR_TEMPLATE
                          .formatted(reservation.getReservationId(), reservation.getStatus()),
                    command)
        );
    }

    private void markAbortedIfNeeded(CancelFundsReservationCommand cancelFundsReservationCommand) {
        if (!abortedTransferRepository.existsByTransferIdAndCustomerId(cancelFundsReservationCommand.getTransferId(),
              cancelFundsReservationCommand.getCustomerId())) {
            abortedTransferRepository.save(new AbortedTransfer(cancelFundsReservationCommand.getTransferId(),
                  cancelFundsReservationCommand.getCustomerId()));
        }
    }

    private DomainEvent toDomainEvent(Object accountEvent) {
        return Stream.of(AccountEventType.values())
              .filter(e -> e.getPayloadType()
                    .equals(accountEvent.getClass()))
              .findFirst()
              .map(type -> new DomainEvent(type.name(), accountEvent))
              .orElseThrow(() -> new IllegalStateException(UNSUPPORTED_ACCOUNT_EVENT
                    .formatted(accountEvent.getClass())));
    }
}
