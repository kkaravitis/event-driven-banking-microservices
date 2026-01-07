package com.wordpress.kkaravitis.banking.transfer.adapter.outbound.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@AllArgsConstructor
@Getter
@Entity
@Table(name = "outbox_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "messageId")
@ToString
public class OutboxMessage {

    @Id
    @Column(name = "message_id", nullable = false, updatable = false, length = 255)
    private String messageId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "message_type", nullable = false, length = 255)
    private String messageType;

    @Column(name = "reply_topic", nullable = false, length = 255)
    private String replyTopic;

    /**
     * Stored as JSONB in Postgres.
     * Keep it as String in the entity; serialize your DTO to JSON before persisting.
     */
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "destination_topic", nullable = false, length = 255)
    private String destinationTopic;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onInsert() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
