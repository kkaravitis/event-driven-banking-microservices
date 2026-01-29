package com.wordpress.kkaravitis.banking.account.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "aborted_transfer")
@EqualsAndHashCode(of = {"transferId"})
public class AbortedTransfer {

    @Id
    @Column(name = "transfer_id", nullable = false, updatable = false)
    private UUID transferId;

    @Column(name = "aborted_at", nullable = false, updatable = false)
    private Instant abortedAt;

    public AbortedTransfer(UUID transferId) {
        this.transferId = transferId;
        this.abortedAt = Instant.now();
    }
}
