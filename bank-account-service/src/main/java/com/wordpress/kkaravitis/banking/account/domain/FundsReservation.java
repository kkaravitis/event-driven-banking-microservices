package com.wordpress.kkaravitis.banking.account.domain;

import com.wordpress.kkaravitis.banking.account.domain.types.ReleaseReason;
import com.wordpress.kkaravitis.banking.account.domain.types.ReservationStatus;
import com.wordpress.kkaravitis.banking.account.domain.values.DomainErrorCode;
import com.wordpress.kkaravitis.banking.account.domain.values.DomainResult;
import com.wordpress.kkaravitis.banking.account.domain.values.FinalizeOutcome;
import com.wordpress.kkaravitis.banking.account.domain.values.ReleaseOutcome;
import com.wordpress.kkaravitis.banking.account.domain.values.Transition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "funds_reservation")
public class FundsReservation {

    @Id
    @Column(name = "reservation_id", nullable = false, updatable = false)
    private String reservationId;

    @Column(name = "transfer_id", nullable = false, updatable = false)
    private UUID transferId;

    @Column(name = "from_account_id", nullable = false, updatable = false)
    private String fromAccountId;

    @Column(name = "to_account_id", nullable = false, updatable = false)
    private String toAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_reason")
    private ReleaseReason releaseReason; // null until released

    @Version
    private long version;

    private FundsReservation(String reservationId,
          UUID transferId,
          String fromAccountId,
          String toAccountId,
          BigDecimal amount,
          String currency) {
        this.reservationId = reservationId;
        this.transferId = transferId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = ReservationStatus.ACTIVE;
        this.releaseReason = null;
    }

    public static FundsReservation createNew(String reservationId,
          UUID transferId,
          String fromAccountId,
          String toAccountId,
          BigDecimal amount,
          String currency) {
        return new FundsReservation(reservationId, transferId, fromAccountId, toAccountId, amount, currency);
    }

    public boolean isCancelled() {
        return status == ReservationStatus.RELEASED && releaseReason == ReleaseReason.CANCELLED;
    }

    /**
     * ACTIVE -> FINALIZED
     * FINALIZED -> idempotent ok
     * RELEASED -> fail
     */
    public DomainResult<FinalizeOutcome> finalizeTransfer(Account fromAccount, Account toAccount) {
        ReservationStatus current = this.status;

        if (current == ReservationStatus.RELEASED) {
            return DomainResult.fail(
                  DomainErrorCode.RESERVATION_RELEASED,
                  "Cannot finalize reservation %s because it is RELEASED".formatted(reservationId)
            );
        }

        if (current == ReservationStatus.FINALIZED) {
            return DomainResult.ok(
                  new FinalizeOutcome(transferId),
                  new Transition(current.name(), current.name())
            );
        }

        // current == ACTIVE
        if (fromAccount == null || toAccount == null) {
            return DomainResult.fail(DomainErrorCode.CURRENCY_MISMATCH, "Accounts must be provided");
        }

        // Validate both mutations first (no partial updates)
        DomainResult<Void> v1 = fromAccount.validateConsumeReserved(amount, currency);
        if (!v1.isValid()) {
            return DomainResult.fail(v1.getError().code(), v1.getError().message());
        }

        DomainResult<Void> v2 = toAccount.validateCredit(amount, currency);
        if (!v2.isValid()) {
            return DomainResult.fail(v2.getError().code(), v2.getError().message());
        }

        // Apply mutations
        DomainResult<Void> r1 = fromAccount.consumeReserved(amount, currency);
        if (!r1.isValid()) {
            return DomainResult.fail(r1.getError().code(), r1.getError().message());
        }

        DomainResult<Void> r2 = toAccount.credit(amount, currency);
        if (!r2.isValid()) {
            return DomainResult.fail(r2.getError().code(), r2.getError().message());
        }

        this.status = ReservationStatus.FINALIZED;

        return DomainResult.ok(
              new FinalizeOutcome(transferId),
              new Transition(current.name(), this.status.name())
        );
    }

    /**
     * ACTIVE -> RELEASED(NORMAL)
     * RELEASED -> idempotent ok (keeps reason)
     * FINALIZED -> fail
     */
    public DomainResult<ReleaseOutcome> release(Account fromAccount) {
        return releaseInternal(fromAccount, ReleaseReason.NORMAL);
    }

    /**
     * ACTIVE -> RELEASED(CANCELLED)
     * RELEASED -> idempotent ok (upgrades reason to CANCELLED)
     * FINALIZED -> fail
     */
    public DomainResult<ReleaseOutcome> cancel(Account fromAccount) {
        return releaseInternal(fromAccount, ReleaseReason.CANCELLED);
    }

    private DomainResult<ReleaseOutcome> releaseInternal(Account fromAccount, ReleaseReason requestedReason) {
        ReservationStatus current = this.status;

        if (current == ReservationStatus.FINALIZED) {
            return DomainResult.fail(
                  DomainErrorCode.RESERVATION_FINALIZED,
                  "Cannot release reservation %s because it is FINALIZED".formatted(reservationId)
            );
        }

        if (current == ReservationStatus.RELEASED) {
            // idempotent; “upgrade” NORMAL -> CANCELLED if cancel arrives late
            if (requestedReason == ReleaseReason.CANCELLED && this.releaseReason != ReleaseReason.CANCELLED) {
                this.releaseReason = ReleaseReason.CANCELLED;
            }
            return DomainResult.ok(
                  new ReleaseOutcome(transferId, reservationId, this.releaseReason),
                  new Transition(current.name(), current.name())
            );
        }

        // current == ACTIVE
        if (fromAccount == null) {
            return DomainResult.fail(DomainErrorCode.CURRENCY_MISMATCH, "From account must be provided");
        }

        DomainResult<Void> v = fromAccount.validateReleaseReserved(amount, currency);
        if (!v.isValid()) return DomainResult.fail(v.getError().code(), v.getError().message());

        DomainResult<Void> r = fromAccount.releaseReserved(amount, currency);
        if (!r.isValid()) return DomainResult.fail(r.getError().code(), r.getError().message());

        this.status = ReservationStatus.RELEASED;
        this.releaseReason = requestedReason;

        return DomainResult.ok(
              new ReleaseOutcome(transferId, reservationId, this.releaseReason),
              new Transition(current.name(), this.status.name())
        );
    }
}
