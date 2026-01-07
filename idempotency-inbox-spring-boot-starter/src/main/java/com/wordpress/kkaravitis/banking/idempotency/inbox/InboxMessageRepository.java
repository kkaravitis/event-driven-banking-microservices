package com.wordpress.kkaravitis.banking.idempotency.inbox;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {

    Optional<InboxMessage> findByMessageId(String messageId);

    boolean existsByMessageId(String messageId);

    @Modifying
    @Query("""
        delete from InboxMessage m
        where m.receivedAt < :threshold
    """)
    int deleteOlderThan(@Param("threshold") Instant threshold);
}
