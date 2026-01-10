package com.wordpress.kkaravitis.banking.idempotency.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "inbox_message")
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

    public InboxMessage(String messageId) {
        this.messageId = messageId;
        this.receivedAt = Instant.now();
    }
}
