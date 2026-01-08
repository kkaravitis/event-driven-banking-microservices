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
import java.util.List;
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

    public AggregateResult startCompletion(String fundsReservationId) {
        TransferState currentState = state;

        if (currentState == TransferState.REQUESTED) {
            this.fundsReservationId = fundsReservationId;
            state = TransferState.COMPLETION_PENDING;

            return AggregateResult.builder()
                  .aggregateId(id)
                  .transition(new Transition(currentState.name(),
                        state.name()))
                  .build();
        }

        if (currentState == TransferState.COMPLETION_PENDING) {

            return AggregateResult.builder()
                  .aggregateId(id)
                  .transition(new Transition(currentState.name(),
                        state.name()))
                  .build();
        }

        if (List.of(TransferState.CANCEL_PENDING,
              TransferState.CANCELLED
              ).contains(currentState)) {

            return AggregateResult.builder()
                  .aggregateId(id)
                  .error(new DomainError(DomainErrorCode.COMPLETE_TOO_LATE,
                        String.format(TRANSITION_ERROR_TEMPLATE, id,
                              currentState, TransferState.COMPLETION_PENDING)))
                  .build();
        }

        if (List.of(TransferState.COMPLETED,
                    TransferState.REJECTED)
              .contains(currentState)) {

            return AggregateResult.builder()
                  .aggregateId(id)
                  .error(new DomainError(DomainErrorCode.UNEXPECTED_TRANSITION,
                        String.format(TRANSITION_ERROR_TEMPLATE, id,
                              currentState, TransferState.COMPLETION_PENDING)))
                  .build();
        }

        return illegalStateError();
    }

    public AggregateResult markCompleted() {
        TransferState currentState = state;

        if (currentState == TransferState.COMPLETION_PENDING) {
            state = TransferState.COMPLETED;
            return successfulTransition(currentState);
        }

        if (currentState == TransferState.COMPLETED) {
            return successfulTransition(currentState);
        }

        if (List.of(TransferState.CANCELLED,
                    TransferState.CANCEL_PENDING)
              .contains(currentState)) {
            return transitionError(DomainErrorCode.COMPLETE_TOO_LATE,
                  TransferState.COMPLETED);
        }

        if (currentState == TransferState.REQUESTED) {
            return transitionError(DomainErrorCode.UNEXPECTED_TRANSITION,
                  TransferState.COMPLETED);
        }

        if (currentState == TransferState.REJECTED) {
            return transitionError(DomainErrorCode.UNEXPECTED_TRANSITION,
                  TransferState.COMPLETED);
        }

        return illegalStateError();
    }

    public AggregateResult reject() {
        TransferState currentState = state;

        if (List.of(TransferState.COMPLETION_PENDING,
                    TransferState.REQUESTED)
              .contains(currentState)) {
            state = TransferState.REJECTED;

            return AggregateResult.builder()
                  .aggregateId(id)
                  .transition(new Transition(currentState.name(),
                        this.state.name()))
                  .build();
        }

        if (currentState == TransferState.REJECTED) {

            return AggregateResult.builder()
                  .aggregateId(id)
                  .transition(new Transition(currentState.name(),
                        currentState.name()))
                  .build();
        }

        if (List.of(TransferState.CANCEL_PENDING,
                    TransferState.CANCELLED)
              .contains(currentState)) {

            return AggregateResult.builder()
                  .aggregateId(id)
                  .error(new DomainError(DomainErrorCode.REJECT_TOO_LATE,
                        String.format(TRANSITION_ERROR_TEMPLATE,
                              id, currentState, TransferState.REJECTED)))
                  .build();
        }

        if (currentState == TransferState.COMPLETED) {
            return AggregateResult.builder()
                  .aggregateId(id)
                  .error(new DomainError(DomainErrorCode.UNEXPECTED_TRANSITION,
                        String.format(TRANSITION_ERROR_TEMPLATE,
                              id, currentState, TransferState.REJECTED)))
                  .build();
        }

        return illegalStateError();
    }

    public AggregateResult startCancellation() {
        TransferState currentState = state;
        if (currentState == TransferState.REQUESTED) {
            state = TransferState.CANCEL_PENDING;
            return AggregateResult.builder()
                  .aggregateId(id)
                  .transition(new Transition(currentState.name(),
                        state.name()))
                  .build();
        }

        if (currentState == TransferState.CANCEL_PENDING) {
            return AggregateResult.builder()
                  .aggregateId(id)
                  .transition(new Transition(currentState.name(),
                        state.name()))
                  .build();
        }

        if (List.of(TransferState.REJECTED,
                    TransferState.COMPLETED,
                    TransferState.COMPLETION_PENDING)
              .contains(currentState)) {
            return AggregateResult.builder()
                  .aggregateId(id)
                  .error(new DomainError(DomainErrorCode.CANCEL_TOO_LATE,
                        String.format(TRANSITION_ERROR_TEMPLATE, id,
                              currentState, TransferState.CANCEL_PENDING)))
                  .build();
        }

        if (currentState == TransferState.CANCELLED) {
            return AggregateResult.builder()
                  .aggregateId(id)
                  .error(new DomainError(DomainErrorCode.UNEXPECTED_TRANSITION,
                        String.format(TRANSITION_ERROR_TEMPLATE, id,
                              currentState, TransferState.CANCEL_PENDING)))
                  .build();
        }

        return illegalStateError();
    }

    public AggregateResult markCancelled() {
        TransferState currentState = state;

        if (currentState == TransferState.CANCEL_PENDING) {
            state = TransferState.CANCELLED;
            return AggregateResult.builder()
                  .aggregateId(id)
                  .transition(new Transition(currentState.name(),
                        state.name()))
                  .build();
        }

        if (currentState == TransferState.CANCELLED) {
            return AggregateResult.builder()
                  .aggregateId(id)
                  .transition(new Transition(currentState.name(),
                        state.name()))
                  .build();
        }

        if (List.of(TransferState.REQUESTED,
                    TransferState.REJECTED,
                    TransferState.COMPLETED,
                    TransferState.COMPLETION_PENDING
              ).contains(currentState)) {
            return AggregateResult.builder()
                  .aggregateId(id)
                  .error(new DomainError(DomainErrorCode.UNEXPECTED_TRANSITION,
                        String.format(TRANSITION_ERROR_TEMPLATE,
                              id, currentState, TransferState.CANCELLED)))
                  .build();
        }

        return illegalStateError();
    }

    private AggregateResult transitionError(DomainErrorCode code, TransferState toState) {
        return AggregateResult.builder()
              .aggregateId(id)
              .error(new DomainError(DomainErrorCode.COMPLETE_TOO_LATE,
                    String.format(TRANSITION_ERROR_TEMPLATE, id,
                          state, toState)))
              .build();
    }

    private AggregateResult illegalStateError() {
        return AggregateResult.builder()
              .aggregateId(id)
              .error(new DomainError(DomainErrorCode.ILLEGAL_STATE, String
                    .format(ILLEGAL_STATE_ERROR_TEMPLATE, id, state)))
              .build();
    }

    private AggregateResult successfulTransition(TransferState fromState) {
        return AggregateResult.builder()
              .aggregateId(id)
              .transition(new Transition(fromState.name(),
                    state.name()))
              .build();
    }

}
