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

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "aborted_at", nullable = false, updatable = false)
    private Instant abortedAt;

    public AbortedTransfer(UUID transferId, String customerId) {
        this.transferId = transferId;
        this.customerId = customerId;
        this.abortedAt = Instant.now();
    }
}
