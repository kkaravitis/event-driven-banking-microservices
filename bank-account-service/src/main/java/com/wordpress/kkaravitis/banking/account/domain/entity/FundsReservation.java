package com.wordpress.kkaravitis.banking.account.domain.entity;

import com.wordpress.kkaravitis.banking.account.domain.type.DomainErrorCode;
import com.wordpress.kkaravitis.banking.account.domain.type.ReleaseReason;
import com.wordpress.kkaravitis.banking.account.domain.type.ReservationStatus;
import com.wordpress.kkaravitis.banking.account.domain.value.DomainResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static FundsReservation createNew(String reservationId,
          UUID transferId,
          String fromAccountId,
          String toAccountId,
          BigDecimal amount,
          String currency) {
        return new FundsReservation(reservationId, transferId, fromAccountId, toAccountId, amount, currency);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public boolean isCancelled() {
        return status == ReservationStatus.RELEASED
              && releaseReason == ReleaseReason.CANCELLED;
    }

    public DomainResult finalizeTransfer(Account fromAccount, Account toAccount) {
        ReservationStatus currentStatus = this.status;

        if (currentStatus == ReservationStatus.RELEASED) {
            return DomainResult.fail(
                  DomainErrorCode.RESERVATION_RELEASED,
                  "Cannot finalize reservation %s because it is RELEASED"
                        .formatted(reservationId)
            );
        }

        if (currentStatus == ReservationStatus.FINALIZED) {
            return DomainResult.ok();
        }

        if (fromAccount == null || toAccount == null) {
            return DomainResult.fail(DomainErrorCode.INVALID_ACCOUNT,
                  "Accounts must be provided");
        }

        DomainResult domainResult = fromAccount.transfer(amount, currency, toAccount);
        if (!domainResult.isValid()) {
            return domainResult;
        }

        this.status = ReservationStatus.FINALIZED;

        return DomainResult.ok();
    }

    public DomainResult release(Account fromAccount) {
        return releaseInternal(fromAccount, ReleaseReason.NORMAL);
    }

    public DomainResult cancel(Account fromAccount) {
        return releaseInternal(fromAccount, ReleaseReason.CANCELLED);
    }

    private DomainResult releaseInternal(Account fromAccount, ReleaseReason requestedReason) {
        ReservationStatus current = this.status;

        if (current == ReservationStatus.FINALIZED) {
            return DomainResult.fail(
                  DomainErrorCode.RESERVATION_FINALIZED,
                  "Cannot release reservation %s because it is FINALIZED"
                        .formatted(reservationId)
            );
        }

        if (current == ReservationStatus.RELEASED) {
            if (requestedReason == ReleaseReason.CANCELLED && this.releaseReason != ReleaseReason.CANCELLED) {
                this.releaseReason = ReleaseReason.CANCELLED;
            }
            return DomainResult.ok();
        }

        if (fromAccount == null) {
            return DomainResult.fail(DomainErrorCode.INVALID_ACCOUNT,
                  "From account is not provided for funds reservation with id %s and transfer id %s"
                        .formatted(reservationId, transferId));
        }

        DomainResult domainResult = fromAccount.releaseReserved(amount, currency);
        if (!domainResult.isValid()) {
            return DomainResult.fail(domainResult.getError().code(),
                  domainResult.getError().message());
        }

        this.status = ReservationStatus.RELEASED;
        this.releaseReason = requestedReason;

        return DomainResult.ok();
    }
}
