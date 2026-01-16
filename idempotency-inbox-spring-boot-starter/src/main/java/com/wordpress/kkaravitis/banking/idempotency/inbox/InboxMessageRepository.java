package com.wordpress.kkaravitis.banking.idempotency.inbox;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {
    @Modifying
    @Query(
          value = """
                INSERT INTO inbox_message (message_id, received_at)
                VALUES (:messageId, now())
                ON CONFLICT (message_id) DO NOTHING
                """,
          nativeQuery = true
    )
    int insertIfAbsent(@Param("messageId") String messageId);

    @Modifying
    @Query("""
        delete from InboxMessage m
        where m.receivedAt < :threshold
    """)
    int deleteOlderThan(@Param("threshold") Instant threshold);
}
