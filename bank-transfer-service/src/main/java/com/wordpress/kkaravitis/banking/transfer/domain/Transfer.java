package com.wordpress.kkaravitis.banking.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Version;

@Getter
@Entity
@EqualsAndHashCode(of = {"id"})
@ToString
@Table(name = "transfer")
public class Transfer {
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Transfer() { }

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
        this.state = TransferState.PENDING;
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

    public DomainResult complete() {
        TransferState currentState = state;

        if (List.of(TransferState.CANCELLED, TransferState.REJECTED).contains(currentState)) {
            return DomainResult.builder()
                  .errors(List.of(new DomainError(DomainErrorCode.COMPLETE_TOO_LATE,
                        String
                              .format("Transfer %s failed to transit to COMPLETED state from state %s",
                                    id,
                                    currentState))))
                  .build();
        }

        if (currentState == TransferState.COMPLETED) {
            return DomainResult.builder()
                  .transition(new Transition(TransferState.COMPLETED.name(),
                        TransferState.COMPLETED.name()))
                  .build();
        }

        if (List.of(TransferState.PENDING, TransferState.CANCEL_PENDING).contains(currentState)) {
            state = TransferState.COMPLETED;
            return DomainResult.builder()
                  .transition(new Transition(currentState.name(),
                        state.name()))
                  .build();
        }

        return DomainResult.builder()
              .errors(List.of(new DomainError(DomainErrorCode.ILLEGAL_STATE, String
                    .format("Transfer %s is in illegal state %s", id, this))))
              .build();

    }

    public DomainResult reject() {
        TransferState currentState = state;

        if (List.of(TransferState.PENDING,
              TransferState.CANCEL_PENDING,
              TransferState.CANCELLED).contains(currentState)) {

            state = TransferState.REJECTED;
            return DomainResult.builder()
                  .errors(List.of(new DomainError(DomainErrorCode.REJECT_TOO_LATE,
                        String.format("Transfer %s failed to transit to REJECTED state from state %s",
                              id, currentState))))
                  .build();
        }

        if (currentState == TransferState.REJECTED) {
            return DomainResult.builder()
                  .transition(new Transition(currentState.name(),
                        currentState.name()))
                  .build();
        }

        if (currentState == TransferState.PENDING) {
            this.state = TransferState.REJECTED;

            return DomainResult.builder()
                  .transition(new Transition(currentState.name(),
                        this.state.name()))
                  .build();
        }

        return DomainResult.builder()
              .errors(List.of(new DomainError(DomainErrorCode.ILLEGAL_STATE, String
                    .format("Transfer %s is in illegal state %s", id, this))))
              .build();
    }

    public DomainResult markCancelled() {
        TransferState currentState = state;

        if (List.of(TransferState.PENDING,
                    TransferState.CANCEL_PENDING)
              .contains(currentState)) {
            state = TransferState.CANCELLED;
            return DomainResult.builder()
                  .transition(new Transition(currentState.name(),
                        state.name()))
                  .build();
        }

        if (currentState == TransferState.CANCELLED) {
            return DomainResult.builder()
                  .transition(new Transition(currentState.name(),
                        state.name()))
                  .build();
        }

        if (List.of(TransferState.REJECTED, TransferState.COMPLETED).contains(currentState)) {
            return DomainResult.builder()
                  .errors(List.of(new DomainError(DomainErrorCode.CANCEL_TOO_LATE,
                        String.format("Transfer %s failed to transit to CANCELLED state from state %s",
                        id, currentState))))
                  .build();
        }

        return DomainResult.builder()
              .errors(List.of(new DomainError(DomainErrorCode.ILLEGAL_STATE, String
                    .format("Transfer %s is in illegal state %s", id, this))))
              .build();
    }

    public DomainResult startCancellation() {
            TransferState currentState = state;
            if (currentState == TransferState.PENDING) {
                state = TransferState.CANCEL_PENDING;
                return DomainResult.builder()
                      .transition(new Transition(currentState.name(),
                            state.name()))
                      .build();
            }

            if (currentState == TransferState.CANCEL_PENDING) {
                return DomainResult.builder()
                      .transition(new Transition(currentState.name(),
                            state.name()))
                      .build();
            }

            if (List.of(TransferState.REJECTED,
                        TransferState.COMPLETED,
                        TransferState.CANCELLED)
                  .contains(currentState)) {
                return DomainResult.builder()
                      .errors(List.of(new DomainError(DomainErrorCode.CANCEL_TOO_LATE,
                            String.format("Transfer %s failed to transit to CANCEL_PENDING state from state %s",
                                  id, currentState))))
                      .build();
            }

        return DomainResult.builder()
              .errors(List.of(new DomainError(DomainErrorCode.ILLEGAL_STATE, String
                    .format("Transfer %s is in illegal state %s", id, this))))
              .build();
    }


}
