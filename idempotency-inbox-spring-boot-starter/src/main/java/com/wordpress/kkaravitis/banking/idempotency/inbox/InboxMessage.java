package com.wordpress.kkaravitis.banking.idempotency.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "inbox_message",
    uniqueConstraints = @UniqueConstraint(name = "ux_inbox_message_message_id", columnNames = "message_id")
)
public class InboxMessage {

    @Id
    @SequenceGenerator(
        name = "inbox_message_id_gen",
        sequenceName = "inbox_message_seq",
        allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inbox_message_id_gen")
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "message_id", nullable = false, length = 256, updatable = false)
    private String messageId;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected InboxMessage() {
        // for JPA
    }

    public InboxMessage(String messageId) {
        this.messageId = messageId;
        this.receivedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
