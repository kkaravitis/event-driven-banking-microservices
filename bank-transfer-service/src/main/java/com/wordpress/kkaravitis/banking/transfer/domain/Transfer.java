package com.wordpress.kkaravitis.banking.transfer.domain;

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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = {"id"})
@ToString
@Table(name = "transfer")
public class Transfer {
    private static final String ILLEGAL_STATE_ERROR_TEMPLATE = "Transfer %s is in illegal state %s";

    private static final String TRANSITION_ERROR_TEMPLATE = "Transfer %s was not allowed to transit from state %s to state %s";

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "from_account_id", nullable = false)
    private String fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private String toAccountId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private TransferState state;

    @Column(name = "funds_reservation_id")
    private String fundsReservationId;

    @Version
    @Column(name = "version")
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Transfer(UUID id,
          String fromAccountId,
          String toAccountId,
          BigDecimal amount,
          String currency) {
        this.id = id;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.currency = currency;
        this.state = TransferState.REQUESTED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Transfer createNew(UUID id,
          String fromAccountId,
          String toAccountId,
          BigDecimal amount,
          String currency) {
        return new Transfer(id, fromAccountId, toAccountId, amount, currency);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public DomainResult startCompletion(String fundsReservationId) {
        TransferState currentState = state;
        return switch (currentState) {
            case REQUESTED -> {
                this.fundsReservationId = fundsReservationId;
                state = TransferState.COMPLETION_PENDING;
                yield success(currentState);
            }

            case COMPLETION_PENDING -> success(currentState);

            case CANCEL_PENDING, CANCELLED ->
                  transitionError(DomainErrorCode.COMPLETE_TOO_LATE, TransferState.COMPLETION_PENDING);

            case COMPLETED, REJECTED ->
                  transitionError(DomainErrorCode.UNEXPECTED_TRANSITION, TransferState.COMPLETION_PENDING);

            default -> illegalStateError();
        };
    }

    public DomainResult markCompleted() {
        TransferState currentState = state;
        return switch (currentState) {
            case COMPLETION_PENDING -> {
                state = TransferState.COMPLETED;
                yield success(currentState);
            }
            case COMPLETED -> success(currentState);

            case CANCELLED, CANCEL_PENDING ->
                  transitionError(DomainErrorCode.COMPLETE_TOO_LATE, TransferState.COMPLETED);

            case REQUESTED, REJECTED ->
                  transitionError(DomainErrorCode.UNEXPECTED_TRANSITION, TransferState.COMPLETED);

            default -> illegalStateError();
        };
    }

    public DomainResult reject() {
        TransferState currentState = state;
        return switch(currentState) {
            case COMPLETION_PENDING, REQUESTED -> {
                state = TransferState.REJECTED;
                yield success(currentState);
            }

            case REJECTED -> success(currentState);

            case CANCEL_PENDING, CANCELLED -> transitionError(DomainErrorCode.REJECT_TOO_LATE, TransferState.REJECTED);

            case COMPLETED -> transitionError(DomainErrorCode.UNEXPECTED_TRANSITION, TransferState.REJECTED);

            default -> illegalStateError();
        };
    }

    public DomainResult startCancellation() {
        TransferState currentState = state;
        return switch (currentState) {
            case REQUESTED -> {
                state = TransferState.CANCEL_PENDING;
                yield success(currentState);
            }

            case CANCEL_PENDING -> success(currentState);

            case REJECTED, COMPLETED, COMPLETION_PENDING ->
                  transitionError(DomainErrorCode.CANCEL_TOO_LATE, TransferState.CANCEL_PENDING);

            case CANCELLED ->
                  transitionError(DomainErrorCode.UNEXPECTED_TRANSITION, TransferState.CANCEL_PENDING);

            default -> illegalStateError();
        };
    }

    public DomainResult markCancelled() {
        TransferState currentState = state;
        return switch (currentState) {
            case CANCEL_PENDING -> {
                state = TransferState.CANCELLED;
                yield success(currentState);
            }

            case CANCELLED -> success(currentState);

            case REQUESTED, REJECTED, COMPLETED, COMPLETION_PENDING ->
                  transitionError(DomainErrorCode.UNEXPECTED_TRANSITION, TransferState.CANCELLED);

            default -> illegalStateError();
        };
    }

    private DomainResult transitionError(DomainErrorCode code, TransferState toState) {
        return DomainResult.builder()
              .aggregateId(id)
              .error(new DomainError(code,
                    String.format(TRANSITION_ERROR_TEMPLATE, id,
                          state, toState)))
              .build();
    }

    private DomainResult illegalStateError() {
        return DomainResult.builder()
              .aggregateId(id)
              .error(new DomainError(DomainErrorCode.ILLEGAL_STATE, String
                    .format(ILLEGAL_STATE_ERROR_TEMPLATE, id, state)))
              .build();
    }

    private DomainResult success(TransferState fromState) {
        return DomainResult.builder()
              .aggregateId(id)
              .transition(new Transition(fromState.name(),
                    state.name()))
              .build();
    }

}
