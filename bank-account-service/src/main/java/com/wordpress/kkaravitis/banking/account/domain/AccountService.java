package com.wordpress.kkaravitis.banking.account.domain;

import com.wordpress.kkaravitis.banking.account.api.commands.CancelFundsReservationCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.FinalizeTransferCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.ReleaseFundsCommand;
import com.wordpress.kkaravitis.banking.account.api.commands.ReserveFundsCommand;
import com.wordpress.kkaravitis.banking.account.api.events.AccountEventType;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReleaseFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReleasedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationCancellationRejectedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationCancelledEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservationFailedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.FundsReservedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.TransferApprovalFailedDueToCancelEvent;
import com.wordpress.kkaravitis.banking.account.api.events.TransferApprovalFailedEvent;
import com.wordpress.kkaravitis.banking.account.api.events.TransferFinalizedEvent;
import com.wordpress.kkaravitis.banking.account.domain.entity.AbortedTransfer;
import com.wordpress.kkaravitis.banking.account.domain.entity.Account;
import com.wordpress.kkaravitis.banking.account.domain.entity.FundsReservation;
import com.wordpress.kkaravitis.banking.account.domain.repository.AbortedTransferRepository;
import com.wordpress.kkaravitis.banking.account.domain.repository.AccountRepository;
import com.wordpress.kkaravitis.banking.account.domain.repository.FundsReservationRepository;
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

    private final AccountRepository accountRepository;
    private final FundsReservationRepository reservationRepository;
    private final AbortedTransferRepository abortedTransferRepository;

    public DomainEvent reserveFunds(ReserveFundsCommand command) {
        UUID transferId = command.getTransferId();

        if (abortedTransferRepository.existsById(transferId)) {
            return toDomainEvent(new FundsReservationFailedDueToCancelEvent(transferId,
                  TRANSFER_HAD_BEEN_CANCELLED));
        }

        Optional<FundsReservation> existingReservation = reservationRepository.findByTransferId(transferId);
        if (existingReservation.isPresent()) {
            FundsReservation reservation = existingReservation.get();

            if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                return toDomainEvent(new FundsReservedEvent(transferId, reservation.getReservationId()));
            }

            if (reservation.isCancelled()) {
                return toDomainEvent(new FundsReservationFailedDueToCancelEvent(transferId,
                      TRANSFER_HAD_BEEN_CANCELLED));
            }

            return toDomainEvent(new FundsReservationFailedEvent(
                  transferId,
                  "Reservation already exists in state " + reservation.getStatus()
            ));
        }

        Account from = accountRepository.findById(command.getFromAccountId())
              .orElse(null);
        if (from == null) {
            return toDomainEvent(new FundsReservationFailedEvent(transferId,
                  "From account not found"));
        }

        Account to = accountRepository.findById(command.getToAccountId()).orElse(null);
        if (to == null) {
            return toDomainEvent(new FundsReservationFailedEvent(transferId,
                  "To account not found"));
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

    public DomainEvent releaseFunds(ReleaseFundsCommand command) {
        UUID transferId = command.getTransferId();

        FundsReservation reservation = reservationRepository.findById(command.getReservationId())
              .orElseThrow(() ->
                    new IllegalStateException("Reservation not found: "
                          + command.getReservationId()));

        if (abortedTransferRepository.existsById(transferId) || reservation.isCancelled()) {
            return toDomainEvent(new FundsReleaseFailedDueToCancelEvent(transferId,
                  reservation.getReservationId()));
        }

        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return toDomainEvent(new FundsReleasedEvent(transferId, reservation.getReservationId()));
        }

        Account from = accountRepository.findById(reservation.getFromAccountId())
              .orElseThrow(() -> new IllegalStateException("From account not found: "
                    + reservation.getFromAccountId()));

        DomainResult result = reservation.release(from);

        if (!result.isValid()) {
            throw new IllegalStateException("Release failed: "
                  + result.getError().message());
        }

        return toDomainEvent(new FundsReleasedEvent(transferId, reservation.getReservationId()));
    }

    public DomainEvent finalizeTransfer(FinalizeTransferCommand command) {
        UUID transferId = command.getTransferId();

        FundsReservation reservation = reservationRepository.findById(command.getReservationId())
              .orElse(null);

        if (reservation == null) {
            return toDomainEvent(new TransferApprovalFailedEvent(transferId,
                  "Reservation not found"));
        }

        if (abortedTransferRepository.existsById(transferId) || reservation.isCancelled()) {
            return toDomainEvent(new TransferApprovalFailedDueToCancelEvent(transferId,
                  TRANSFER_HAD_BEEN_CANCELLED));
        }

        if (reservation.getStatus() == ReservationStatus.FINALIZED) {
            return toDomainEvent(new TransferFinalizedEvent(transferId));
        }

        Account from = accountRepository.findById(reservation.getFromAccountId()).orElse(null);
        Account to = accountRepository.findById(reservation.getToAccountId()).orElse(null);

        if (from == null || to == null) {
            return toDomainEvent(new TransferApprovalFailedEvent(transferId,
                  "Account(s) not found"));
        }

        DomainResult result = reservation.finalizeTransfer(from, to);

        if (!result.isValid()) {
            return toDomainEvent(new TransferApprovalFailedEvent(transferId,
                  result.getError().message()));
        }

        return toDomainEvent(new TransferFinalizedEvent(transferId));
    }

    @Transactional
    public DomainEvent cancelFundsReservation(CancelFundsReservationCommand command) {
        UUID transferId = command.getTransferId();

        markAbortedIfNeeded(transferId);

        Optional<FundsReservation> opt = reservationRepository.findByTransferId(transferId);
        if (opt.isEmpty()) {
            return toDomainEvent(new FundsReservationCancelledEvent(transferId));
        }

        FundsReservation reservation = opt.get();
        if (reservation.getStatus() == ReservationStatus.FINALIZED) {
            return toDomainEvent(new FundsReservationCancellationRejectedEvent(transferId));
        }

        Account from = accountRepository.findById(reservation.getFromAccountId())
              .orElse(null);
        if (from == null) {
            return toDomainEvent(new FundsReservationCancellationRejectedEvent(transferId));
        }

        DomainResult domainResult = reservation.cancel(from);

        if (!domainResult.isValid()) {
            return toDomainEvent(new FundsReservationCancellationRejectedEvent(transferId));
        }

        return toDomainEvent(new FundsReservationCancelledEvent(transferId));
    }

    private void markAbortedIfNeeded(UUID transferId) {
        if (!abortedTransferRepository.existsById(transferId)) {
            abortedTransferRepository.save(new AbortedTransfer(transferId));
        }
    }

    private DomainEvent toDomainEvent(Object accountEvent) {
        return Stream.of(AccountEventType.values())
              .filter(e -> e.getPayloadType()
                    .equals(accountEvent.getClass()))
              .findFirst()
              .map(type -> new DomainEvent(type.name(), accountEvent))
              .orElseThrow(() -> new IllegalStateException("Unsupported Account Event of class : "
                    + accountEvent.getClass()));
    }
}
